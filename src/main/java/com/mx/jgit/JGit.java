package com.mx.jgit;

import com.mx.jgit.configuration.GitConfig;
import com.mx.jgit.configuration.GitCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
public class JGit {

    @Autowired
    GitCredentials gitCredentials;

    @Autowired
    GitConfig gitConfig;

    private int MAX_COMMITS_PER_DAY = 4;

    public static void sourceGit() throws GitAPIException, IOException {

        String sourceRepoPath = "/Users/fernando.ramirez/Documents/Develop/github/Organizations/FalcoLombardi-MX/OCA-Java-SE8-Certification/.git";
        File fSourceRepoPath = new File(sourceRepoPath);
        Git git = Git.open(fSourceRepoPath);

        Iterable<RevCommit> log = git.log().call();

        log.forEach(commit -> {
            PersonIdent authorIdent = commit.getAuthorIdent();
            Date authorDate = authorIdent.getWhen();
            TimeZone authorTimeZone = authorIdent.getTimeZone();

            System.out.println("authorDate " + authorDate.toString());

        });




    }


    public void destinationGit() throws GitAPIException, IOException {
        String destinationRepoPath = "/Users/fernando.ramirez/Documents/Develop/github/seeand/OCA-Java-SE8-Certification/.git";
        File fdestinationRepoPath = new File(destinationRepoPath);
        Git destinationGit = Git.open(fdestinationRepoPath);

        Date date = new GregorianCalendar(2015, Calendar.JUNE, 19).getTime();

        System.out.println("gitCredentials" +  gitCredentials.toString());
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(
                gitCredentials.getUsername(),
                gitCredentials.getPassword()
        );

        PersonIdent defaultCommitter = new PersonIdent(destinationGit.getRepository());
        PersonIdent committer = new PersonIdent(defaultCommitter, date);

        System.out.println("commit date: " + date.toString());
        destinationGit.add().addFilepattern(".").call();
        destinationGit.commit()
                .setMessage("Commit with time trough JGit " + date.toString())
                .setCommitter(committer)
                .call();

        destinationGit.push().setCredentialsProvider(cp).call();

        log.info("Pushed");
    }

    @PostConstruct
    public void gitReset() throws IOException, GitAPIException, CheckoutConflictException, URISyntaxException {

        File fSourceRepoPath = new File(gitConfig.getSourceDir().concat("/.git"));
        Git git = Git.open(fSourceRepoPath);

        Iterable<RevCommit> revCommits =  git.log().all().call();

        List<RevCommit> result = new ArrayList<>();
        List<Integer> ommitedCommits = new ArrayList<>();
        HashMap<String,Integer> tempCommits = new LinkedHashMap<>();
        revCommits.iterator().forEachRemaining(result::add);
        Collections.reverse(result);

        log.info("Commits size: {} ", result.size());
        int commitCounter = 1;

        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(
                gitCredentials.getUsername(),
                gitCredentials.getPassword()
        );


        for (int i = 0; i < result.size(); i++) {
            String sCommitDate = result.get(i).getCommitterIdent().getWhen().toString();

            if(!tempCommits.containsKey(sCommitDate)) {
                tempCommits.put(sCommitDate,1);
            }
            else {
                tempCommits.put(sCommitDate,(tempCommits.get(sCommitDate) + 1));
            }

            if (tempCommits.get(sCommitDate) > MAX_COMMITS_PER_DAY) {
                ommitedCommits.add(i);
            }
        }

        DO_COMMIT: for (RevCommit c :result) {

            String shaCommitID = c.getName();
            Date commitDate = c.getCommitterIdent().getWhen();
            String commitMessage = c.getFullMessage();

            log.info("Trying to reset to: {}, {}, {}",
                    shaCommitID,
                    commitDate.toString(),
                    commitMessage
            );

            if (ommitedCommits.contains(commitCounter)) {
                log.info("Commit ommited by user: {}",shaCommitID);
                continue DO_COMMIT;
            }

            Ref ref = git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef(shaCommitID)
                    .call();

            log.info("Copy directories -> \nSource: {} \nDestinarion: {}",
                    gitConfig.getSourceDir(),
                    gitConfig.getDestinationDir()
            );

            FileUtils.copyDirectory(
                new File(gitConfig.getSourceDir()),
                new File(gitConfig.getDestinationDir()),
                getFileFilter(),
                true
            );

            log.info("Folder copy DONE! {}", commitCounter);

            gitPush(
                Git.open(new File(gitConfig.getDestinationDir())),
                commitDate,
                commitMessage,
                cp
            );
            //break;
            commitCounter++;
        }
        log.info("Total of commits: {}", commitCounter-1);
    }

    public void gitPush(Git git, Date commitDate, String commitMessage, CredentialsProvider credentialsProvider) throws GitAPIException {

        log.info("Trying to pushing ");

        PersonIdent defaultCommitter = new PersonIdent(git.getRepository());
        PersonIdent committer = new PersonIdent(defaultCommitter, commitDate);

        git.add().addFilepattern(".").call();

        git.commit()
            .setMessage(commitMessage)
            .setCommitter(committer)
            .call();

        log.info("Commited...");

        /*git.push()
            .setRemote(gitCredentials.getUrl())
            .setCredentialsProvider(credentialsProvider)
            .call();*/

        log.info("Pushed...");
    }

    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File file) {
                if (!file.getName().equals(".git")) {
                    return true;
                }
                return false;
            }
        };
    }
}

