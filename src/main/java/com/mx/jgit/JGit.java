package com.mx.jgit;

import com.mx.jgit.configuration.GitConfig;
import com.mx.jgit.configuration.GitCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class JGit {

    @Autowired
    GitCredentials gitCredentials;

    @Autowired
    GitConfig gitConfig;

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

        System.out.println("pushed");
    }

    @PostConstruct
    public void gitReset() throws IOException, GitAPIException, CheckoutConflictException {

        File fdestinationRepoPath = new File(gitConfig.getSourceDir());
        Git git = Git.open(fdestinationRepoPath);

        Iterable<RevCommit> revCommits =  git.log().all().call();

        List<RevCommit> result = new ArrayList<>();
        revCommits.iterator().forEachRemaining(result::add);
        Collections.reverse(result);

        for (RevCommit c :result) {

            String shaCommitID = c.getName();
            Date commitDate = c.getCommitterIdent().getWhen();

            log.info("Trying to reset to: {}, {}", shaCommitID, commitDate.toString());
            Ref ref = git.reset().setMode(ResetCommand.ResetType.HARD).setRef(shaCommitID).call();

            FileUtils.copyDirectory(
                new File(gitConfig.getCopySourceDir()),
                new File(gitConfig.getCopyDestinationDir()),
                getFileFilter(),
                true
            );
            System.out.println("Folder copy DONE!");

            break;
        }
    }

    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File file) {
                if (file.getName().endsWith(".git")) {
                    return true;
                }
                return false;
            }
        };
    }

}

