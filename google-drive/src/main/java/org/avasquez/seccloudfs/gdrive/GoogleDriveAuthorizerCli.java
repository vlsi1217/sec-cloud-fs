package org.avasquez.seccloudfs.gdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.avasquez.seccloudfs.utils.CliUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Simple command-line app used to authorize the Sec Cloud FS to access Google Drive accounts through OAuth2.
 * Authorization information is then stored in the DB for later use by the system.
 *
 * @author avasquez
 */
public class GoogleDriveAuthorizerCli {

    private static final String CONTEXT_PATH = "application-context.xml";

    private BufferedReader stdIn;
    private PrintWriter stdOut;
    private GoogleDriveAuthorizationSupport authSupport;
    private GoogleDriveCredentialRepository credentialRepository;

    public GoogleDriveAuthorizerCli(BufferedReader stdIn, PrintWriter stdOut,
                                    GoogleDriveAuthorizationSupport authSupport,
                                    GoogleDriveCredentialRepository credentialRepository) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;
        this.authSupport = authSupport;
        this.credentialRepository = credentialRepository;
    }

    public void run() {
        String authUrl = authSupport.getAuthorizationUrl();
        String code = null;

        stdOut.println("1. Go to: " + authUrl);
        stdOut.print("2. Enter authorization code: ");
        stdOut.flush();

        try {
            code = CliUtils.readLine(stdIn, stdOut);
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to read authorization code", e, stdOut);
        }

        try {
            GoogleDriveCredential credential = authSupport.exchangeCode(code);

            credentialRepository.insert(credential);

            stdOut.println("Credential successfully obtained and stored in DB with ID '" + credential.getId() + "'");
            stdOut.println();
        } catch (IOException e) {
            CliUtils.die("ERROR: Unable to exchange authorization code for credential", e, stdOut);
        } catch (DbException e) {
            CliUtils.die("ERROR: Unable to store credential in DB", e, stdOut);
        }
    }

    public static void main(String... args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXT_PATH);
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter stdOut = new PrintWriter(System.out);
        GoogleDriveAuthorizationSupport authSupport = context.getBean(GoogleDriveAuthorizationSupport.class);
        GoogleDriveCredentialRepository credentialRepository = context.getBean(GoogleDriveCredentialRepository.class);
        GoogleDriveAuthorizerCli cli = new GoogleDriveAuthorizerCli(stdIn, stdOut, authSupport, credentialRepository);

        cli.run();
    }

}
