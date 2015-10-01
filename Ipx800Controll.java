import java.io.*;
import java.net.*;

public class Ipx800Control
{
	protected int port = 9870;
	protected String server = "domus.bboett.lan";
	protected String returnMsg = "";

	protected boolean sendCmd(String cmd)
	{
		Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try
		{
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.println(cmd);
			returnMsg = in.readLine();
			System.out.println(in.readLine());

			out.close();
			in.close();
			socket.close();
		}
		catch (UnknownHostException e)
		{
            System.err.println("Don't know about host " + server);
            return false;
        }
		catch (IOException e)
		{
            System.err.println("Couldn't get I/O for the connection");
            return false;
        }


		return true;
	}
	/*
	 Setxxy ou  xx  est  le  numéro  de  sortie  de  01  à  32 
	 et  y  l’état  de  la  sortie    0  =  Off  et  1  =  On 
	 Pour  mettre  le  relais  1  à  1  le  code  commande  est  Set011       
	 Pour  commander  la  sortie  en  mode  impulsionnel  il  faut  ajouter  le  
	 symbole  p  en  fin  de  chaine  et   avoir  été  définit  les  délais  
	 Ta  et  Tb  dans  l’interface  WEB  de  l’IPX800. 
	 Pour mettre le relais 1 à 1 en mode impulsionnel  le code commande est Set011p  
	 */
	public boolean set(int relai, boolean etat) {return set(relai,etat,false);}
	public boolean set(int relai, boolean etat, boolean impulsion)
	{
		String cmd = "Set";
		if (relai < 1 || relai > 32) return false;
		if(relai < 10) cmd += "0"; 
		cmd += relai;
		if (etat) cmd += "1";
		else cmd += "0";
		if (impulsion) cmd += "p";
		return(sendCmd(cmd));
	}
	 /*           Commander les sorties  simultanément    :  
	 Bit= Le  mode  bitmask  vous  permet  de  définir  avec  une  seule  commande  
	 l’état  que  doivent  prendre  des  32  sorties. Le  code  Bit=  doit  etre  
	 suivi  des  32  états  des  sorties  (0  ou  1) 
	 Bit=00000000000000000000000000000000    
	 Met  les  32  sorties  à  0 Bit=11111111111111111111111111111111  
	 Met  les  32  sorties  à    1 
	 */
	public boolean bit(boolean[] relais)
	{
		if (relais == null ||relais.length <= 0) return false;
		
		String cmd = "Bit=";
		for(int i = 0; i < Math.min(32,relais.length); i++)
		   if (relais[i]) cmd += "1";
		   else cmd += "0";
		
		return(sendCmd(cmd));
	}
	/*
	 Obtenir l‘état d’une entrée:  
	 GetIn Paramètres : GetInx ou x est ne numéro de l’entrée de 1 à 32 GetIn1 
	 permet d’obtenir l’état de l’entrée 1. 
	 L’IPX800 répond GetIn1=0  (Valeur  0 ou 1 en fonction de l’état de l‘entrée).
	 	 */
	public boolean getIn(int relai)
	{
		if (relai <1 ||relai > 32) return false;

		String cmd = "GetIn"+relai;
		
		boolean retVal = false;
		
		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			if(part2.equals("1")) retVal = true;
		}
	
		return retVal;
	}
	/*
	 
	 ••••                        Obtenir l‘état de toutes les entrées : 
	 GetInputs Paramètres : GetInputs permet de recevoir dans une seule trame
	 l’état des 32 entrées digitales. 
	 Réponse de l’IPX800: GetInputs=00000000000000000000000000000000 
	 (Le dernier caractère reçu correspond à  l’entrée 32)            
	 */
	public boolean[] getIn()
	{
		String cmd = "GetInputs";

		boolean[] retVal = new boolean[32];

		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			for(int i =0; i<32;i++)
			{
				if(part2.charAt(i) == '1') retVal[i] = true;
				else retVal[i] = false;
			}
			
		}
		else
		{
			for(int i =0; i<32;i++)
			{
				retVal[i] = false;
			}
		}

		return retVal;
	}
	/*
	 
	 ••••                        Obtenir l‘état d’une entrée analogique :  
	 GetAn Paramètres : 
	   GetAnx ou x est le numéro de l’entrée analogique de 1 à 4 
	   GetAn1 permet de recevoir l’état de l’entrée analogique 1. 
	   Réponse de l’IPX800 GetAn1=512  (valeur de 0 à 1023)           
	 */
	public int getAn(int relai)
	{
		if (relai <1 ||relai > 4) return -1;

		String cmd = "GetAn"+relai;

		int retVal = -1;

		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			try
			{
				retVal = Integer.parse(part2);
			}
			catch(NumberFormatException e)
			{
				System.err.println("no number: "+part2);
			}
		}

		return retVal;
	}
	/*
	 
	   ••••                        Obtenir l‘état d’un compteur d’implusion :  
	   GetCount Paramètres : GetCountx ou x est le numéro de compteur (de 1 à 3) 
	   GetCount1 renvoi la valeur du compteur 1. 
	   Réponse de l’IPX GetCount1=0 (Compteur 32 Bits soit une valeur de 0 à 4294967295)
	 */
	public int getCount(int relai)
	{
		if (relai <1 ||relai > 3) return -1;

		String cmd = "GetCount"+relai;

		int retVal = -1;

		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			try
			{
				retVal = Integer.parse(part2);
			}
			catch(NumberFormatException e)
			{
				System.err.println("no number: "+part2);
			}
		}
		return retVal;
	}
	/*
	 
	    Obtenir l‘état d’une sortie:  
	   GetOut Paramètres : GetOutx ou x est le numéro de la sortie (de 1 à 32). 
	   GetOut1 renvoi la valeur de la sorite 1. 
	   Réponse de l’IPX800 GetOut1=1 (Valeur 0 ou 1 en fonction de l’état de la sortie). 
	 */
	public boolean getOut(int relai)
	{
		if (relai <1 ||relai > 32) return false;

		String cmd = "GetOut"+relai;

		boolean retVal = false;

		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			if(part2.equals("1")) retVal = true;
		}

		return retVal;
	}
	/*
	 
	   ••••                        Obtenir l‘état de toute les  sorties:  
	   GetOutputs Paramètres : 
	   GetOutputs   permet de recevoir dans une seule trame l’état des 32 sorties. 
	   Réponse de l’IPX800: GetOutputs=00000000000000000000000000000000  
	   (Le dernier caractère reçu correspond à la sortie 32)            
	 */
	public boolean[] getOut()
	{
		String cmd = "GetOutputs";

		boolean[] retVal = new boolean[32];

		if(sendCmd(cmd))
		{
			String[] parts = returnMsg.split("=");
			//String part1 = parts[0]; // Getin
			String part2 = parts[1]; // value
			for(int i =0; i<32;i++)
			{
				if(part2.charAt(i) == '1') retVal[i] = true;
				else retVal[i] = false;
			}

		}
		else
		{
			for(int i =0; i<32;i++)
			{
				retVal[i] = false;
			}
		}

		return retVal;
	}
	/*
	 
	   ••••                        Remise à zéro des compteurs:  
	   ResetCount Paramètres : 
	   ResetCountx ou x est le numéro du compteur à remettre à zéro. 
	   ResetCount1 remet le compteur 1 à zéro. 
	   Réponse de l’IPX800: Success •• • •                  
	 */
	public boolean resetCount(int relai)
	{
		if (relai <1 ||relai > 3) return false;

		String cmd = "ResetCount"+relai;

		boolean retVal = false;

		if(sendCmd(cmd) && returnMsg.equals("Success"))
			 retVal = true;

		return retVal;
	}
	/*
	 
	   Reset système de l’IPX800  Reset Paramètres  : 
	   Reset L’envoi de cette commande provoque le redémarrage de l’IPX800 (Reboot). 
	   
	*/
	public void reset()
	{
		String cmd = "Reset";
		sendCmd(cmd);
	}
}
