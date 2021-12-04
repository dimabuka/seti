package project.ooad.truckers.game.model;

import project.ooad.truckers.game.model.SnakesProto.GameMessage;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.ErrorMsg;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.PingMsg;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.StateMsg;
import project.ooad.truckers.game.model.events.EventListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ClientServer extends NetNode implements EventListener {
    private GameModel gameModel;

    private long PING_PERIOD;

    private InetAddress serverIp;
    private int serverPort;

    private Timer pingTimerTask;

    public ClientServer(long PING_PERIOD) throws IOException {
        gameModel = GameModel.getInstance();

        this.PING_PERIOD = PING_PERIOD;

        gameModel.getNetEventManager().subscribe((java.util.EventListener) this,  "receivedPacket");

        System.out.println("Client object created");
    }

    public InetAddress getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    private void setServerIp(InetAddress serverIp) {
        this.serverIp = serverIp;
    }

    private void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void update(String eventType, DatagramPacket receivedPacket) {
        try {
            GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength()));

            if (gameMessage.hasState()) {
                StateMsg stateMsg = gameMessage.getState();

                if (gameModel.isChangeServer()) {
                    System.out.println("Получил последнее состояние умирающего сервера");

                    gameModel.getSnakes().addAll(stateMsg.getState().getSnakesList());
                    gameModel.getFood().addAll(stateMsg.getState().getFoodsList());
                    gameModel.getPlayers().addAll(stateMsg.getState().getPlayers().getPlayersList());

                    gameModel.startServerLogic();
                    gameModel.sendNewServerToEveryone();
                    System.out.println("Отправляю сообщение остальным живым игрокам, что теперь я являюсь новым сервером");
                    gameModel.setChangeServer(false);
                }
                gameModel.getEventManager().notify("gameState", stateMsg.getState().getSnakesList(),
                        stateMsg.getState().getFoodsList(), stateMsg.getState().getPlayers().getPlayersList());

                sendConfirmation(receivedPacket.getAddress(), receivedPacket.getPort(), receivedPacket);
            } else if (gameMessage.hasRoleChange()) {
                GameMessage.RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();

                if (roleChangeMsg.hasReceiverRole() && !roleChangeMsg.hasSenderRole()) {
                    gameModel.setNodeRole(roleChangeMsg.getReceiverRole());

                    System.out.println("Теперь я стал " + roleChangeMsg.getReceiverRole());
                }

                if (roleChangeMsg.hasReceiverRole() && roleChangeMsg.hasSenderRole()) {
                    if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.MASTER &&
                    roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                        gameModel.setNodeRole(roleChangeMsg.getReceiverRole());
                        gameModel.setChangeServer(true);
                        System.out.println("Принял сообщение о том, что мне необходимо стать сервервом. Тепреь я сервер");
                    } else if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.MASTER &&
                            roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.NORMAL) {
                        if (gameModel.getClientServer() != null) {
                            System.out.println("Получил сообщение о том, что у нас новый сервер");
                            gameModel.getClientServer().setServerIp(receivedPacket.getAddress());
                            gameModel.getClientServer().setServerPort(receivedPacket.getPort());
                        }
                    }
                }
            } else if(gameMessage.hasAck()) {
//                System.out.println("[CLIENT_MESSAGE] Confirmation came from the server in receiving message with seq [" +
//                        gameMessage.getMsgSeq() + "]");
                gameModel.clientConfirmMessageWithSeq(gameMessage.getMsgSeq());

                if (gameMessage.getMsgSeq() == 0) {
                    serverIp = receivedPacket.getAddress();
                    serverPort = receivedPacket.getPort();

                    startPingTimerTask();
                }
            } else if(gameMessage.hasError()) {
                ErrorMsg errorMsg = gameMessage.getError();
                gameModel.getEventManager().notify("errorMessage", errorMsg.getErrorMessage());

                sendConfirmation(receivedPacket.getAddress(), receivedPacket.getPort(), receivedPacket);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startPingTimerTask() {
        pingTimerTask = new Timer();
        pingTimerTask.schedule(new PingTimerTask(), 0, PING_PERIOD);
    }

    public void stopPingTimerTask() {
        pingTimerTask.cancel();
        pingTimerTask.purge();
    }

    public class PingTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                gameModel.sendMessage(serverIp, serverPort, createPingMsg());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private GameMessage createPingMsg() {
            GameMessage.Builder gameMessage = GameMessage.newBuilder();

            gameMessage.setPing(PingMsg.newBuilder().build());
            gameMessage.setMsgSeq(gameModel.getClientServerMsgCount());

            gameModel.increaseClientServerMsgCount();

            return gameMessage.build();
        }
    }
}
