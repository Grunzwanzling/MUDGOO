package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientThread extends Thread {

	public Socket clientSocket;
	public int clientID;
	OutputStream out;
	InputStream in;
	public BufferedReader breader;

	public ClientThread(Socket s, int i) {
		clientSocket = s;
		clientID = i;
	}

	@SuppressWarnings("resource")
	public void run() {

		Server.log("New Client: ID: " + clientID + " IP: "
				+ clientSocket.getInetAddress().getHostName());

		try {
			out = clientSocket.getOutputStream();
			in = clientSocket.getInputStream();

			BufferedReader buffr = new BufferedReader(new InputStreamReader(in));
			DataOutputStream buffw = new DataOutputStream(out);
			// ObjectInputStream ois = new ObjectInputStream(
			// clientSocket.getInputStream());
			// ObjectOutputStream oos = new ObjectOutputStream(
			// clientSocket.getOutputStream());

			String ClientVersion = buffr.readLine();

			if (Server.CompatibleClientVersion.startsWith(ClientVersion)) {

				String username = buffr.readLine();

				// Command: REGISTER
				try {
					if (username.startsWith("REGISTER;")) {

						username = username.substring(8, username.length());
						System.out.println(username);
						String newUser = username.substring(1, username
								.indexOf(";", username.indexOf(";") + 1));
						String newPassword = username.substring(username
								.indexOf(";", username.indexOf(";") + 1) + 1,
								username.length());

						if (!Server.passwords.containsKey(newUser)) {
							if (!newPassword.contains("=")
									&& !newPassword.contains(";")
									&& !newUser.contains(";")
									&& !newPassword.contains("=")) {
								Server.passwords
										.setProperty(newUser, newPassword);
								OutputStream out = Files.newOutputStream(Paths
										.get(new File(Server.settings
												.getProperty("userFile"))
												.getCanonicalPath()));
								Server.passwords.store(out, null);
								Server.log(newUser + " registered with password "
										+ newPassword);
							} else {
								buffw.writeBytes("= and ; is not allowed!");
								Server.log(newUser
										+ " tried to register with password "
										+ newPassword
										+ "but he had symbols in it");
							}
						} else {
							buffw.writeBytes("Username already taken!");
							Server.log(newUser
									+ " tried to register with password "
									+ newPassword
									+ "but username was already taken.");
						}
					}

				} catch (StringIndexOutOfBoundsException e) {
					Server.log("Client " + clientID
							+ " is sending a wrong command: REGISTER;"
							+ username);
				}
				String password = buffr.readLine();

				if (username != null && password != null) {
					if (Server.passwords.containsKey(username)) {
						if (password.equals(Server.passwords
								.getProperty(username))) {
							Server.log("Client " + clientID
									+ " logged in with username: " + username
									+ " and password: " + password);
							buffw.writeBytes("Password accepted\n");

							while (true) {

								String command = buffr.readLine();
								if (command != null) {
									// Chat.log("Client " + clientID + " ("
									// + username + ") is sending: \""
									// + command + "\"");
									// Command EXISTS

									if (command.startsWith("EXISTS;")) {
										try {
											if (Server.passwords
													.containsKey(command
															.substring(
																	7,
																	command.length()))) {
												System.out.println("true");
												buffw.writeBytes("true\n");
											} else {
												System.out.println("false");
												buffw.writeBytes("false\n");
											}
										} catch (StringIndexOutOfBoundsException e) {
											Server.log("Client "
													+ clientID
													+ " is sending a wrong command: EXISTS;"
													+ username);

										}
									}

									// Command: SEND

									if (command.startsWith("SEND;")) {
										try {
											command = command.substring(4,
													command.length());
											String messageTo = command
													.substring(
															1,
															command.indexOf(
																	";",
																	command.indexOf(";") + 1));
											String message = command
													.substring(
															command.indexOf(
																	";",
																	command.indexOf(";") + 1) + 1,
															command.length());
											if (Server.passwords
													.containsKey(messageTo)) {
												File dir = new File(
														Server.settings
																.getProperty("chatDir"));
												if (!dir.exists())
													dir.mkdir();
												System.out
														.println("Client "
																+ clientID
																+ " ("
																+ username
																+ ") is sending a message to \""
																+ messageTo
																+ "\" Message: \""
																+ message
																+ "\"");

												File users = new File(
														Server.settings
																.getProperty("chatDir")
																+ "\\"
																+ messageTo
																+ ".txt");

												if (!users.exists())
													users.createNewFile();

												FileWriter out = new FileWriter(
														users, true);
												out.append((username + ":"
														+ message + "\n"));
												out.close();
											} else {
												buffw.writeBytes("User doesn't exist");
											}
										} catch (StringIndexOutOfBoundsException e) {
											Server.log("Client "
													+ clientID
													+ " is sending a wrong command: SEND;"
													+ username);
										}
									}

									// Command: RECEIVE

									if (command.startsWith("RECEIVE")) {
										
										File dir = new File(
												Server.settings
														.getProperty("chatDir"));
										if (!dir.exists())
											dir.mkdir();
										File in = new File(
												Server.settings
														.getProperty("chatDir")
														+ "\\"
														+ username
														+ ".txt");
										if (!in.exists())
											in.createNewFile();
										int q = 0;

										BufferedReader br = new BufferedReader(
												new FileReader(in));
										while (br.readLine() != null)
											q++;

										br = new BufferedReader(new FileReader(
												in));
										String line;
										int w = 0;
										String data[] = new String[q];
										while ((line = br.readLine()) != null) {

											data[w] = line;
											w++;
										}
										if (data.length > 0)
											buffw.writeBytes(data[0] + "\n");
										else
											buffw.writeBytes("nomessage\n");
										br.close();

										BufferedWriter bw = new BufferedWriter(
												new FileWriter(in));

										for (int i = 1; i < q; i++) {

											bw.write(data[i] + "\n");

										}

										bw.close();

									}

								}
							}

						} else {

							buffw.writeBytes("Wrong username or password!\n");
							clientSocket.close();
							System.out
									.println("Client "
											+ clientID
											+ " disconnected because of wrong username or password");
						}

					} else {
						buffw.writeBytes("Wrong username or password!\n");
						clientSocket.close();
						Server.log("Client " + clientID
								+ " disconnected because of wrong username");
					}

				} else {
					buffw.writeBytes("Incomplete login data!\n");
					clientSocket.close();
					Server.log("Client " + clientID
							+ " disconnected because of incomplete login data");
				}

			} else {
				buffw.writeBytes("Only version " + Server.CompatibleClientVersion
						+ " is allowed!\n");
				clientSocket.close();
				Server.log("Client "
						+ clientID
						+ " disconnected because of incompatible ClientVersion "
						+ ClientVersion);
			}

		} catch (SocketException e) {
			Server.log("Client " + clientID + " disconnected");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
