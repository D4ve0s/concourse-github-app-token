package eu.neufeldt.concoursegithubcredentials.in;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.neufeldt.concoursegithubcredentials.GithubClient;
import eu.neufeldt.concoursegithubcredentials.model.InRequest;
import eu.neufeldt.concoursegithubcredentials.model.TokenResponse;
import eu.neufeldt.concoursegithubcredentials.model.VersionWrapper;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class In {

    private final static Gson GSON = new Gson();

    private final GithubClient client;
    private final FileSystem fileSystem;
    private final InstallationIdFinder installationIdFinder;

    public In(GithubClient client, FileSystem fileSystem) {
        this.client = client;
        this.fileSystem = fileSystem;
        this.installationIdFinder = new InstallationIdFinder(client);
    }

    public String getToken(InRequest request, String destinationDirectory) throws IOException, InterruptedException {
        int installationId = installationIdFinder.find(request.source.user, request.source.org);
        JsonElement repositories = request.source.repositories;
        JsonElement permissions = request.source.permissions;
        if (request.params != null) {
            if (request.params.repositories != null) repositories = request.params.repositories;
            if (request.params.permissions != null) permissions = request.params.permissions;
        }
        TokenResponse tokenResponse;
        if (repositories != null || permissions != null) {
            JsonObject body = new JsonObject();
            if (repositories != null) body.add("repositories", repositories);
            if (permissions != null) body.add("permissions", permissions);
            tokenResponse = client.call("POST", "app/installations/" + installationId + "/access_tokens", GSON.toJson(body), TokenResponse.class);
        } else {
            tokenResponse = client.call("POST", "app/installations/" + installationId + "/access_tokens", TokenResponse.class);
        }
        Path path = fileSystem.getPath(destinationDirectory);
        Files.createDirectories(path);
        Path tokenFile = path.resolve("token");
        Files.writeString(tokenFile, tokenResponse.token);
        return GSON.toJson(new VersionWrapper(request.version));
    }
}
