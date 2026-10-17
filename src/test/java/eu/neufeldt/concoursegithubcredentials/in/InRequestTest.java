package eu.neufeldt.concoursegithubcredentials.in;

import com.google.common.jimfs.Jimfs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.neufeldt.concoursegithubcredentials.GithubClient;
import eu.neufeldt.concoursegithubcredentials.model.InRequest;
import eu.neufeldt.concoursegithubcredentials.model.InstallationResponse;
import eu.neufeldt.concoursegithubcredentials.model.TokenResponse;
import eu.neufeldt.concoursegithubcredentials.model.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InRequestTest {

    private final InRequest.Source source = new InRequest.Source(123, "private key", "tenjaa", null, null, null);
    private final Version version = new Version("someDate");
    private final InRequest inRequest = new InRequest(source, version, null);
    private final Gson gson = new Gson();
    private FileSystem fileSystem;
    private GithubClient client;

    @BeforeEach
    void setUp() {
        client = mock(GithubClient.class);
        fileSystem = Jimfs.newFileSystem();
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void test() throws InterruptedException, IOException {
        // given
        when(client.call("GET", "users/tenjaa/installation", InstallationResponse.class)).thenReturn(new InstallationResponse(456));
        when(client.call("POST", "app/installations/456/access_tokens", TokenResponse.class)).thenReturn(new TokenResponse("v1.b71be873ad96e64a84025ae7bee7694a99cb4ba9"));
        In in = new In(client, fileSystem);

        // when
        String output = in.getToken(inRequest, "destination/directory");

        // then
        assertThat(fileSystem.getPath("destination", "directory", "token")).hasContent("v1.b71be873ad96e64a84025ae7bee7694a99cb4ba9");

        assertThat(output).isEqualTo("{\"version\":{\"date\":\"someDate\"}}");
    }

    @Test
    void scoped() throws InterruptedException, IOException {
        // given
        JsonArray repositories = new JsonArray();
        repositories.add("repo-a");
        JsonObject permissions = new JsonObject();
        permissions.addProperty("contents", "read");
        InRequest.Source scopedSource = new InRequest.Source(123, "private key", "tenjaa", null, repositories, permissions);
        InRequest scopedRequest = new InRequest(scopedSource, version, null);
        when(client.call("GET", "users/tenjaa/installation", InstallationResponse.class)).thenReturn(new InstallationResponse(456));
        when(client.call("POST", "app/installations/456/access_tokens", "{\"repositories\":[\"repo-a\"],\"permissions\":{\"contents\":\"read\"}}", TokenResponse.class)).thenReturn(new TokenResponse("v1.scoped"));
        In in = new In(client, fileSystem);

        // when
        String output = in.getToken(scopedRequest, "destination/directory");

        // then
        assertThat(fileSystem.getPath("destination", "directory", "token")).hasContent("v1.scoped");
        assertThat(output).isEqualTo("{\"version\":{\"date\":\"someDate\"}}");
    }
}
