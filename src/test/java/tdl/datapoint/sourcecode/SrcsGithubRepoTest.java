package tdl.datapoint.sourcecode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

public class SrcsGithubRepoTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    
    @Test
    public void testIsGithubRepoExistsForKeyShouldReturnTrue() {
        
    }
    
}
