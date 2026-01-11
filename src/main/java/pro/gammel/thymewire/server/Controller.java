package pro.gammel.thymewire.server;

import com.sun.net.httpserver.HttpExchange;

public interface Controller {

    /**
     * Priority of the controller defines the order in the lookup sequence: the lower the number, the earlier this
     * controller is called in the queue. Thus, if two controllers match the same path, the one with smaller number
     * will serve it.
     *
     * @return priority of this controller
     */
    int priority();

    boolean accepts(HttpExchange exchange);

    Response respond(HttpExchange exchange);

}
