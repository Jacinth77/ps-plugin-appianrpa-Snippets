package com.appian.rpa.snippets.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.novayre.jidoka.client.api.IJidokaServer;
import com.novayre.jidoka.client.api.IRobot;
import com.novayre.jidoka.client.api.JidokaFactory;
import com.novayre.jidoka.client.api.exceptions.JidokaFatalException;
import com.novayre.jidoka.client.api.exceptions.JidokaItemException;

public class RpaFtpClient {

	private static RpaFtpClient rpaFtpClientInstance;

	/** FTP host */
	private String host;

	/** FTP user name */
	private String username;

	/** FTP password */
	private String password;

	/** FTP active directory, must exist */
	private String workingDirectory;

	/** Folder to store files obtained from FTP */
	private String downloadDirectory;

	/** For internal use */
	private String workingDirBackUp;

	/** Server module */
	private IJidokaServer<?> server;

	private RpaFtpClient() {
		this.server = JidokaFactory.getServer();
		setWorkingDirectory("");
		setDownloadDirectory(System.getProperty("user.dir"));
	}

	/**
	 * Static method to create an instance of the RpaFtpClient class
	 * 
	 * @param robot {@link IRobot} instance
	 * 
	 * @return RpaFtpClient instance
	 */
	public static RpaFtpClient getInstance() {
		if (rpaFtpClientInstance == null) {
			rpaFtpClientInstance = new RpaFtpClient();
		}

		return rpaFtpClientInstance;
	}

	/**
	 * TODO documentar
	 * 
	 * @param host
	 * @param username
	 * @param password
	 */
	public void init(String host, String username, String password) {

		this.username = "";
		this.host = host;
		this.username = username;
		this.password = password;
		this.server = JidokaFactory.getServer();
	}

	/**
	 * TODO documentar
	 * 
	 * @param host
	 * @param username
	 * @param password
	 */
	public void init(String host, String username, String password, String workingDirectory) {

		this.username = "";
		this.host = host;
		this.username = username;
		this.password = password;
		setWorkingDirectory(workingDirectory);
		this.server = JidokaFactory.getServer();
	}

	/**
	 * TODO documentar
	 * 
	 * @param host
	 * @param username
	 * @param password
	 */
	public void init(String host, String username, String password, String workingDirectory, String downloadDirectory) {

		this.host = host;
		this.username = username;
		this.password = password;
		setWorkingDirectory(workingDirectory);
		setDownloadDirectory(downloadDirectory);
		this.server = JidokaFactory.getServer();
	}

	/**
	 * Delete the file in the current active directory with the given name.
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean deleteFile(String fileName) {

		List<String> filesName = new ArrayList<>();

		filesName.add(fileName);

		return deleteFiles(filesName);
	}

	/**
	 * Delete the file list in the current directory with the given names.
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean deleteFiles(List<String> fileNameList) {

		FTPClient ftpClient = connect();

		try {

			for (String fileName : fileNameList) {
				server.debug("Deleting file: " + workingDirectory + fileName);
				if (!ftpClient.deleteFile(workingDirectory + fileName)) {
					return false;
				}
			}

		} catch (IOException e) {
			throw new JidokaItemException("There is an error deleting the file");
		} finally {
			disconnect(ftpClient);
		}

		return true;
	}

	/**
	 * Download the file with the given name.
	 * 
	 * @param fileName
	 * @return
	 */
	public File downloadFile(String fileNameRegExp) {

		return downloadFile(f -> Pattern.matches(fileNameRegExp, f.getName()));
	}

	/**
	 * Download the first file that match the given predicate.
	 * 
	 * @param predicate
	 * @return
	 */
	public File downloadFile(Predicate<FTPFile> predicate) {

		List<File> files = downloadFiles(predicate);

		if (files.isEmpty()) {
			return null;
		}

		return files.get(0);
	}

	/**
	 * Download the file with the given name.
	 * 
	 * @param fileNameRegExp
	 * @return
	 */
	public List<File> downloadFiles(String fileNameRegExp) {

		return downloadFiles(f -> Pattern.matches(fileNameRegExp, f.getName()));
	}

	/**
	 * Download the given file list.
	 * 
	 * @param fileNameList
	 * @return
	 */
	public List<File> downloadFiles(List<String> fileNameList) {

		List<File> localResult = new ArrayList<>();

		for (String fileName : fileNameList) {

			File file = downloadFile(fileName);

			localResult.add(file);
		}

		return localResult;
	}

	/**
	 * Download all files matching the given predicate.
	 * 
	 * @param predicate
	 * @return
	 */
	public List<File> downloadFiles(Predicate<FTPFile> predicate) {

		FTPClient ftpClient = connect();
		List<File> results = new ArrayList<>();

		try {

			FTPFile[] listFiles = ftpClient.listFiles();

			if (listFiles == null) {
				return results;
			}

			for (FTPFile ftpFile : listFiles) {

				if (!ftpFile.isFile()) {
					continue;
				}

				if (!predicate.test(ftpFile)) {
					continue;
				}

				File file = downloadFile(ftpClient, ftpFile);
				results.add(file);
			}

		} catch (JidokaItemException e) {
			throw e;
		} catch (Exception e) {
			throw new JidokaItemException("There is an error downloading the file.");
		} finally {
			disconnect(ftpClient);
		}

		return results;
	}

	/**
	 * Upload the file to the active folder.
	 * 
	 * @param fileToUpload
	 * @return
	 */
	public boolean uploadFile(File fileToUpload) {

		ArrayList<File> toUpload = new ArrayList<>();

		toUpload.add(fileToUpload);

		return uploadFiles(toUpload);
	}

	/**
	 * Upload all files to the active folder.
	 * 
	 * @param filesToUpload
	 * @return
	 */
	public boolean uploadFiles(List<File> filesToUpload) {

		FTPClient ftpClient = connect();

		try {

			for (File file : filesToUpload) {

				if (!uploadFile(ftpClient, file)) {
					return false;
				}
			}

		} catch (JidokaFatalException e) {
			throw e;
		} finally {
			disconnect(ftpClient);
		}

		return true;
	}

	/**
	 * Returns a {@link List} with the name of all the files in the FTP.
	 * 
	 * @return
	 */
	public List<String> getAllFilesName() {

		FTPClient ftpClient = connect();

		server.debug("Obtaining files in the directory: " + workingDirectory);

		try {

			FTPFile[] listFiles = ftpClient.listFiles();

			if (listFiles == null) {
				server.debug("The directory is empty.");
				return new ArrayList<>();
			}

			return Arrays.asList(listFiles).stream()
					.filter(f -> f != null && (!f.getName().equals(".") && !f.getName().equals("..")))
					.map(f -> f.getName()).collect(Collectors.toList());

		} catch (Exception e) {
			throw new JidokaItemException("There is an error retriving the file list from the ftp.");
		} finally {
			disconnect(ftpClient);
		}
	}

	/**
	 * @return the workingDirectory
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * @param workingDirectory the workingDirectory to set
	 */
	public void setWorkingDirectory(String workingDirectory) {

		if (!workingDirectory.endsWith("/")) {
			workingDirectory += "/";
		}
		this.workingDirectory = workingDirectory;
	}

	/**
	 * @return the destinyFolder
	 */
	public String getDownloadFolder() {
		return downloadDirectory;
	}

	/**
	 * @param downloadDirectory the destinyFolder to set
	 */
	public void setDownloadDirectory(String downloadDirectory) {
		if (!downloadDirectory.endsWith("/")) {
			downloadDirectory += "/";
		}

		if (!Files.exists(Paths.get(downloadDirectory))) {

			File dir = new File(downloadDirectory);

			if (!dir.mkdir()) {
				throw new JidokaFatalException("Cannot create directory for downloads: " + dir.getPath());
			}
		}
		this.downloadDirectory = downloadDirectory;
	}

	public boolean mkDirFtp(String dir) {

		setWorkingDirBackup("");

		FTPClient ftpClient = connect();

		try {

			if (!ftpClient.makeDirectory(dir)) {
				return false;
			}

		} catch (Exception e) {
			throw new JidokaItemException("Error creating directory " + dir);
		} finally {
			disconnect(ftpClient);
			restoreWorkDir();
		}

		server.debug("Directory " + dir + " created in the FTP.");

		return true;
	}

	public boolean rmDirFtp(String dir) {
		return rmDirFtp(dir, false);
	}

	public boolean rmDirFtp(String dir, boolean removeFiles) {

		if (removeFiles) {
			removeAllFiles(dir);
		}

		FTPClient ftpClient = connect();

		try {

			if (!ftpClient.removeDirectory(dir)) {
				server.warn("The directory " + dir + " can't be deleted.");
				return false;
			}

		} catch (Exception e) {
			throw new JidokaItemException("Error deleting directory " + dir);
		} finally {
			disconnect(ftpClient);
		}

		server.debug("Folder " + dir + " removed from the FTP.");

		return true;
	}

	public void removeAllFiles(String dir) {

		setWorkingDirBackup(dir);

		try {
			List<String> arrayFtpFiles = getAllFilesName();

			deleteFiles(arrayFtpFiles);

		} finally {

			restoreWorkDir();
		}

	}

	private FTPClient connect() {

		FTPClient ftpClient = null;

		JidokaItemException lastException = null;

		for (int i = 1; i < 5; i++) {

			server.debug(String.format("Connecting to FTP [%s]. Attempt %d", host, i));
			ftpClient = new FTPClient();
			ftpClient.setConnectTimeout(10000);
			ftpClient.setDefaultTimeout(10000);

			try {

				ftpClient.connect(host, 21);
				ftpClient.setSoTimeout(10000);

				if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
					ftpClient.disconnect();
					server.warn("FTP server refused connection.");
					continue;
				}

			} catch (IOException e) {
				lastException = new JidokaItemException("There is an error connecting to FTP");
				continue;
			}

			try {

				ftpClient.enterLocalPassiveMode();
				if (!ftpClient.login(username, password)) {
					server.debug("Login failed");
					continue;
				}

				server.debug("Status -> " + ftpClient.getStatus() + " -> ReplyCode " + ftpClient.getReplyCode());

			} catch (Exception e) {
				lastException = new JidokaItemException("Login error");
				continue;
			}

			if (StringUtils.isNotBlank(workingDirectory)) {

				try {

					if (!ftpClient.changeWorkingDirectory(workingDirectory)) {
						server.debug("Working directory not changed.");
						continue;
					}

				} catch (IOException e) {
					lastException = new JidokaItemException(
							"There is an error changing to the ftp working directory: " + workingDirectory);
					continue;
				}
			}

			return ftpClient;
		}

		if (ftpClient != null) {
			disconnect(ftpClient);
		}

		if (lastException == null) {
			throw new JidokaItemException("FTP connection failed: " + host);
		}

		throw lastException;
	}

	private void disconnect(FTPClient ftpClient) {

		if (ftpClient == null) {
			return;
		}

		if (!ftpClient.isConnected()) {
			return;
		}

		try {

			ftpClient.logout();

		} catch (IOException e) {
			server.warn("FTP logout error", e);
		}

		try {

			ftpClient.disconnect();

		} catch (IOException e) {
			server.warn("Error disconnecting from FTP", e);
		}
	}

	private File downloadFile(FTPClient ftpClient, FTPFile ftpFile) {

		File file = new File(downloadDirectory + ftpFile.getName());
		JidokaItemException lastException = null;

		for (int i = 1; i < 5; i++) {

			try (FileOutputStream stream = new FileOutputStream(file)) {

				if (!ftpClient.retrieveFile(ftpFile.getName(), stream)) {
					continue;
				}

				return file;

			} catch (Exception e) {
				lastException = new JidokaItemException("There is an error downloading the file from the FTP");
			}
		}

		if (lastException == null) {
			throw new JidokaItemException("There is an error downloading the file from the FTP");
		}

		throw lastException;
	}

	private boolean uploadFile(FTPClient ftpClient, File localFile) {

		for (int i = 1; i < 5; i++) {

			try (FileInputStream stream = new FileInputStream(localFile)) {

				if (!ftpClient.storeFile(localFile.getName(), stream)) {
					continue;
				}

				return true;

			} catch (Exception e) {
				server.warn("An error occurred while uploading the file to the FTP. Attempt: " + i + "//4");
			}
		}

		server.warn("An error occurred trying to upload the file " + localFile.getName() + " to the FTP");

		return false;
	}

	private void restoreWorkDir() {
		setWorkingDirectory(workingDirBackUp);
	}

	private void setWorkingDirBackup(String dir) {
		workingDirBackUp = workingDirectory;
		setWorkingDirectory(dir);
	}

}
