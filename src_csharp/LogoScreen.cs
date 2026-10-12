using System;
using UnityEngine;

public class LogoScreen : MainScreen
{
	private int dem;

	public static string strListserver = string.Empty;

	public static bool isLoadServer;

	public static string serverListSource = "DEFAULT";

	public static bool serverListLoading;

	public static bool serverListFailed;

	private static string savedServerHost;

	private static bool firstSelection;

	public LogoScreen()
	{
		try
		{
			sbyte[] array = CRes.loadRMS("listServer");
			sbyte[] array2 = CRes.loadRMS("isIndexServer");
			sbyte[] array3 = CRes.loadRMS("selectedServerHost");
			if (array3 != null)
			{
				savedServerHost = new DataInputStream(array3).readUTF();
			}
			firstSelection = (array2 == null || array2.Length == 0) && savedServerHost == null;
			if (array != null)
			{
				DataInputStream dataInputStream = new DataInputStream(array);
				strListserver = dataInputStream.readUTF();
				if (!firstSelection && savedServerHost == null && array2 != null && array2.Length > 0)
				{
					string[] array4 = strListserver.TrimStart('\ufeff').Trim().Split(',');
					int num = array2[0];
					if (num >= 0 && num < array4.Length)
					{
						string[] array5 = array4[num].Split(':');
						if (array5.Length == 4)
						{
							savedServerHost = array5[1].Trim();
						}
					}
				}
				if (applyServerList(strListserver, firstSelection))
				{
					serverListSource = "RMS";
				}
			}
		}
		catch (Exception ex)
		{
			Debug.LogWarning("Server cache: " + ex.Message);
		}
		if (GameCanvas.IndexServer < 0 || GameCanvas.IndexServer >= GameCanvas.listServer.GetLength(0))
		{
			GameCanvas.IndexServer = 0;
		}
		refreshServerList();
	}

	public override void paint(mGraphics g)
	{
		g.setColor(0);
		g.fillRect(0, 0, GameCanvas.w, GameCanvas.h, mGraphics.isFalse);
	}

	public override void update()
	{
		dem++;
		if (dem > 60)
		{
			dem = 0;
			GameCanvas.login.Show();
		}
	}

	public static void getServerList(string str, bool isFirst)
	{
		applyServerList(str, isFirst);
	}

	public static void refreshServerList()
	{
		if (serverListLoading || Net.www != null)
		{
			return;
		}
		serverListLoading = true;
		TemMidlet.connectHTTP(GameCanvas.linkIP, delegate(string str)
		{
			serverListLoading = false;
			serverListFailed = !applyServerList(str, firstSelection);
			if (!serverListFailed)
			{
				strListserver = str;
				serverListSource = "HTTP";
				firstSelection = false;
				if (GameCanvas.listIp != null)
				{
					GameCanvas.listIp.rebuildList();
				}
			}
		});
	}

	public static bool applyServerList(string str, bool isFirst)
	{
		if (string.IsNullOrEmpty(str))
		{
			return false;
		}
		str = str.TrimStart('\ufeff').Trim();
		string text = savedServerHost;
		if (!isFirst && text == null && GameCanvas.IndexServer >= 0 && GameCanvas.IndexServer < GameCanvas.listServer.GetLength(0))
		{
			text = GameCanvas.listServer[GameCanvas.IndexServer, 1];
		}
		string[] array = str.Split(',');
		int num = 0;
		string[] array2 = array;
		foreach (string text2 in array2)
		{
			if (text2.Trim().Length > 0)
			{
				num++;
			}
		}
		if (num == 0 || num > 128)
		{
			return false;
		}
		string[,] array3 = new string[num, 2];
		int[] array4 = new int[num];
		int[] array5 = new int[num];
		int num2 = 0;
		int num3 = 0;
		string[] array6 = array;
		foreach (string text3 in array6)
		{
			if (text3.Trim().Length != 0)
			{
				string[] array7 = text3.Split(':');
				if (array7.Length != 4 || array7[0].Trim().Length == 0 || array7[1].Trim().Length == 0 || !int.TryParse(array7[2].Trim(), out var result) || result < 1 || result > 65535 || !int.TryParse(array7[3].Trim(), out var result2))
				{
					return false;
				}
				array3[num2, 0] = array7[0].Trim();
				array3[num2, 1] = array7[1].Trim();
				array4[num2] = result;
				array5[num2] = result2;
				if (!isFirst && text == array3[num2, 1])
				{
					num3 = num2;
				}
				num2++;
			}
		}
		GameCanvas.listServer = array3;
		GameCanvas.portServer = array4;
		GameCanvas.langServer = array5;
		GameCanvas.IndexServer = (sbyte)num3;
		GameCanvas.isVN_Eng = array5[num3] == 0;
		saveListServer(str);
		setChangeLang();
		return true;
	}

	public static void saveSelectedServer()
	{
		try
		{
			savedServerHost = GameCanvas.listServer[GameCanvas.IndexServer, 1];
			DataOutputStream dataOutputStream = new DataOutputStream();
			dataOutputStream.writeUTF(savedServerHost);
			CRes.saveRMS("selectedServerHost", dataOutputStream.toByteArray());
			dataOutputStream.close();
			firstSelection = false;
		}
		catch (Exception ex)
		{
			Debug.LogWarning("Server selection: " + ex.Message);
		}
	}

	public static void setChangeLang()
	{
		MainRMS.showAuto = string.Empty;
		LoginScreen.indexInfoLogin = 1;
		if (GameCanvas.w < 200 || GameCanvas.h < 200)
		{
			LoginScreen.indexInfoLogin = 2;
		}
		else if (GameCanvas.isTouch && GameCanvas.w < 380 && GameCanvas.w > 315 && GameCanvas.w < 380)
		{
			LoginScreen.indexInfoLogin = 2;
		}
		GameCanvas.start_Wait_Dialog(T.pleaseWait, null);
		if (!GameCanvas.isVN_Eng)
		{
			GameCanvas.t = new TE();
			if (mSystem.isIP_TrucTiep)
			{
				LoginScreen.logo = mImage.createImage("/interface/logoeip.png");
			}
			else
			{
				LoginScreen.logo = mImage.createImage("/interface/logoe.png");
			}
		}
		else
		{
			GameCanvas.t = new T();
			if (mSystem.isIP_TrucTiep)
			{
				LoginScreen.logo = mImage.createImage("/interface/logoip.png");
			}
			else
			{
				LoginScreen.logo = mImage.createImage("/interface/logo.png");
			}
		}
		List_Server.gI().doSetCaption();
		Usa_Server.setLinkIp();
		IndoServer.setLinkIp();
		GameCanvas.loadCaptionCmd();
		WorldMapScreen.namePos = null;
		GameCanvas.end_Dialog();
	}

	public static void saveListServer(string str)
	{
		DataOutputStream dataOutputStream = new DataOutputStream();
		try
		{
			dataOutputStream.writeUTF(str);
			CRes.saveRMS("listServer", dataOutputStream.toByteArray());
			dataOutputStream.close();
		}
		catch (Exception)
		{
		}
	}
}
