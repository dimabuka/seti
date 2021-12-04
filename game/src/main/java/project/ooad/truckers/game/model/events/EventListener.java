package project.ooad.truckers.game.model.events;

import java.net.DatagramPacket;
import java.util.List;
import java.util.Map;

import project.ooad.truckers.game.model.announcement.IpAddress;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.*;
import project.ooad.truckers.game.model.SnakesProto.GamePlayer;
import project.ooad.truckers.game.model.SnakesProto.GameState.Snake;
import project.ooad.truckers.game.model.SnakesProto.GameState.Coord;


public interface EventListener {
    default void update(String eventType, Map<IpAddress, AnnouncementMsg> availableGames) {

    }

    default void update(String eventType, List<Snake> snakes, List<Coord> food, List<GamePlayer> players) {

    }

    default void update(String eventType, DatagramPacket receivedPacket) {

    }

    default void update(String eventType, String message) {

    }

    default void update(String eventType) {

    }
}
