package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.game.stats.*;
import dk.easv.bll.move.IMove;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

public class ImprovedSneaky implements IBot {
    final int moveTimeMs = 900;
    private String BOT_NAME = getClass().getSimpleName();

    private long time;


    private ImprovedSneaky.GameSimulator createSimulator(IGameState state) {
        ImprovedSneaky.GameSimulator simulator = new ImprovedSneaky.GameSimulator(new GameState());
        simulator.setGameOver(ImprovedSneaky.GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);
        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }

    @Override
    public IMove doMove(IGameState state) {


        return calculateWinningMove(state, moveTimeMs);
    }
    // Plays single games until it wins and returns the first move for that. If iterations reached with no clear win, just return random valid move
    private IMove calculateWinningMove(IGameState state, int maxTimeMs){
        HashMap<Integer, IMove> simMoves = new HashMap<>();
        List<IMove> winMoves = getWinningMoves(state);

        time = System.currentTimeMillis();
        Random rand = new Random();

        int count = 0;
        int hashIndex = 1;
        while (System.currentTimeMillis() < time + maxTimeMs) { // check how much time has passed, stop if over maxTimeMs
            ImprovedSneaky.GameSimulator simulator = createSimulator(state);
            IGameState gs = simulator.getCurrentState();
            List<IMove> moves = gs.getField().getAvailableMoves();
            IMove randomMovePlayer = moves.get(rand.nextInt(moves.size()));
            IMove winnerMove = randomMovePlayer;

            while (simulator.getGameOver()== ImprovedSneaky.GameOverState.Active){ // Game not ended
                simulator.updateGame(randomMovePlayer);

                // Opponent plays randomly
                if (simulator.getGameOver()== ImprovedSneaky.GameOverState.Active){ // game still going
                    moves = gs.getField().getAvailableMoves();
                    IMove randomMoveOpponent = moves.get(rand.nextInt(moves.size()));
                    simulator.updateGame(randomMoveOpponent);
                }
                if (simulator.getGameOver()== ImprovedSneaky.GameOverState.Active){ // game still going
                    moves = gs.getField().getAvailableMoves();
                    randomMovePlayer = moves.get(rand.nextInt(moves.size()));
                }
            }

            if (simulator.getGameOver()== ImprovedSneaky.GameOverState.Win){
                //System.out.println("Found a win, :)");

                simMoves.put(hashIndex, winnerMove);
                hashIndex++;
                //System.out.println(simMoves.size());
                //System.out.println(time - System.currentTimeMillis());

                //System.out.println(results);
            }
            count++;
        }
        //System.out.println("Did not win, just doing random :¨(");
        //List<IMove> moves = state.getField().getAvailableMoves();
        //IMove randomMovePlayer = moves.get(rand.nextInt(moves.size()));


        if(!winMoves.isEmpty())
            return winMoves.get(0);


        IMove move = getMostFrequentValue(simMoves);

        //System.out.println(getMostFrequentValue(simMoves));
        //return randomMovePlayer; // just play randomly if solution not found
        return move;

    }

    /**
     * Get the most frequent value present in a map.
     *
     * @param map
     *          The map to search.
     * @return The most frequent value in the map (or null).
     */
    private static <K, V> V getMostFrequentValue(
            Map<K, V> map) {
        // Make sure we have an entry.
        if (map != null && map.size() > 0) {
            // the entryset from our input.
            Set<Map.Entry<K, V>> entries = map.entrySet();

            // we need a map to hold the count.
            Map<V, Integer> countMap = new HashMap<V, Integer>();
            // iterate the entries.
            for (Map.Entry<K, V> entry : entries) {
                // get the value.
                V value = entry.getValue();

                if (countMap.containsKey(value)) {
                    // if we've seen it before increment the previous
                    // value.
                    countMap.put(value, countMap.get(value) + 1);
                } else {
                    // otherwise, store 1.
                    countMap.put(value, 1);
                }
            }
            // A holder for the maximum.
            V maxV = null;
            for (Map.Entry<V, Integer> count : countMap.entrySet()) {
                if (maxV == null) {
                    maxV = count.getKey();
                    continue;
                }
                if (count.getValue() > countMap.get(maxV)) {
                    maxV = count.getKey();
                }
            }
            return maxV;
        }
        return null;
    }

    // Simplified version of checking if win. Check the GameManager class to see another similar solution
    private boolean isWinningMove(IGameState state, IMove move, String player){
        // Clones the array and all values to a new array, so we don't mess with the game
        String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);

        //Places the player in the game. Sort of a simulation.
        board[move.getX()][move.getY()] = player;

        int startX = move.getX()-(move.getX()%3);
        if(board[startX][move.getY()]==player)
            if (board[startX][move.getY()] == board[startX+1][move.getY()] &&
                    board[startX+1][move.getY()] == board[startX+2][move.getY()])
                return true;

        int startY = move.getY()-(move.getY()%3);
        if(board[move.getX()][startY]==player)
            if (board[move.getX()][startY] == board[move.getX()][startY+1] &&
                    board[move.getX()][startY+1] == board[move.getX()][startY+2])
                return true;


        if(board[startX][startY]==player)
            if (board[startX][startY] == board[startX+1][startY+1] &&
                    board[startX+1][startY+1] == board[startX+2][startY+2])
                return true;

        if(board[startX][startY+2]==player)
            if (board[startX][startY+2] == board[startX+1][startY+1] &&
                    board[startX+1][startY+1] == board[startX+2][startY])
                return true;

        return false;
    }
    // Compile a list of all available winning moves
    private List<IMove> getWinningMoves(IGameState state){
        String player = "1";
        if(state.getMoveNumber()%2==0)
            player="0";

        List<IMove> avail = state.getField().getAvailableMoves();

        List<IMove> winningMoves = new ArrayList<>();
        for (IMove move:avail) {
            if(isWinningMove(state,move,player))
                winningMoves.add(move);
        }
        return winningMoves;
    }


    /*
        The code below is a simulator for simulation of gameplay. This is needed for AI.

        It is put here to make the Bot independent of the GameManager and its subclasses/enums

        Now this class is only dependent on a few interfaces: IMove, IField, and IGameState

        You could say it is self-contained. The drawback is that if the game rules change, the simulator must be
        changed accordingly, making the code redundant.

     */

    @Override
    public String getBotName() {
        return BOT_NAME;
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    public class Move implements IMove {
        int x = 0;
        int y = 0;

        public Move(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImprovedSneaky.Move move = (ImprovedSneaky.Move) o;
            return x == move.x && y == move.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    class GameSimulator {
        private final IGameState currentState;
        private int currentPlayer = 0; //player0 == 0 && player1 == 1
        private volatile ImprovedSneaky.GameOverState gameOver = ImprovedSneaky.GameOverState.Active;

        public void setGameOver(ImprovedSneaky.GameOverState state) {
            gameOver = state;
        }

        public ImprovedSneaky.GameOverState getGameOver() {
            return gameOver;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || 9 <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || 9 <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);

        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard, new ImprovedSneaky.Move(macroX, macroY), "" + currentPlayer))
                    gameOver = ImprovedSneaky.GameOverState.Win;
                else if (isTie(macroBoard, new ImprovedSneaky.Move(macroX, macroY)))
                    gameOver = ImprovedSneaky.GameOverState.Tie;
            }

        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }


        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            //check col
            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + 3 - 1) return true;
            }

            //check row
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }

            //check diagonal
            if (localX == localY) {
                //we're on a diagonal
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }

            //check anti diagonal
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }
    }


}
