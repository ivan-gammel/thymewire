package pro.gammel.thymewire.server;

public record Response(int statusCode, String body, byte[] binaryContent, String contentType, String redirectLocation) {

    public Response(int statusCode, String body) {
        this(statusCode, body, null, "text/html", null);
    }

    public Response(int statusCode, String body, String contentType) {
        this(statusCode, body, null, contentType, null);
    }

    public Response(int statusCode, byte[] binaryContent, String contentType) {
        this(statusCode, null, binaryContent, contentType, null);
    }

    public boolean isBinary() {
        return binaryContent != null;
    }

    public boolean isRedirect() {
        return redirectLocation != null;
    }

    public static Response redirect(String location) {
        return new Response(302, "", null, "text/html", location);
    }

    public static Response defaultResponse() {
        return new Response(404, "Page not found");
    }

}
