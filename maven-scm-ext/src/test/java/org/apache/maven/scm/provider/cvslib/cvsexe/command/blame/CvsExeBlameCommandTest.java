package org.apache.maven.scm.provider.cvslib.cvsexe.command.blame;

import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.manager.ExtScmManager;
import org.apache.maven.scm.manager.ExtScmManagerFactory;
import org.apache.maven.scm.provider.cvslib.AbstractCvsScmTest;
import org.apache.maven.scm.provider.cvslib.CvsScmTestUtils;
import org.codehaus.plexus.PlexusTestCase;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author Evgeny Mandrikov
 */
public class CvsExeBlameCommandTest extends AbstractCvsScmTest {
  @Override
  protected String getModule() {
    return "test-repo/blame/";
  }

  @Override
  protected File getRepository() {
    return PlexusTestCase.getTestFile("/src/test/cvs-repository");
  }

  @Test
  public void test() throws Exception {
    ExtScmManager scmManager = new ExtScmManagerFactory(false).getScmManager();
    CvsScmTestUtils.executeCVS(
        getWorkingDirectory(),
        "-f -d " + getTestFile("src/test/cvs-repository/") + " co " + getModule()
    );
    BlameScmResult result = scmManager.blame(
        getScmRepository(),
        getScmFileSet(),
        getModule() + "src/java/org/apache/maven/MavenUtils.java"
    );
    if (!result.isSuccess()) {
      fail(result.getProviderMessage() + "\n" + result.getCommandOutput());
    }

    List<BlameLine> lines = result.getLines();
    int size = lines.size();
    assertEquals(1110, size);
    assertEquals("1.3", lines.get(size - 1).getRevision());
    assertEquals("brekke", lines.get(size - 1).getAuthor());
  }

}