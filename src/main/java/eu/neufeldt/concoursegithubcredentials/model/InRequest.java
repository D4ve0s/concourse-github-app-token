package eu.neufeldt.concoursegithubcredentials.model;

public class InRequest {
    public final Source source;
    public final Version version;
    public final Params params;

    public InRequest(Source source, Version version, Params params) {
        this.source = source;
        this.version = version;
        this.params = params;
    }

    public static class Source {
        public final int appId;
        public final String privateKey;
        public final String user;
        public final String org;
        public final com.google.gson.JsonArray repositories;
        public final com.google.gson.JsonObject permissions;

        public Source(int appId, String privateKey, String user, String org, com.google.gson.JsonArray repositories, com.google.gson.JsonObject permissions) {
            this.appId = appId;
            this.privateKey = privateKey;
            this.user = user;
            this.org = org;
            this.repositories = repositories;
            this.permissions = permissions;
        }
    }

    public static class Params {
        public final com.google.gson.JsonArray repositories;
        public final com.google.gson.JsonObject permissions;

        public Params(com.google.gson.JsonArray repositories, com.google.gson.JsonObject permissions) {
            this.repositories = repositories;
            this.permissions = permissions;
        }
    }
}
