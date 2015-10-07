/*

 Copyright (c) 2015 Bruno Boettcher. All rights reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This file is part of Foobar.

 Foobar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Foobar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nohkumado.ipx800control;
import java.io.*;
import java.net.*;
import java.security.spec.*;
/**
 classe qui comprend un interfacage entre tous les accˋes TCP/I m2m vers un objet java

 @author Bruno Boettcher <nokumado@gmail.com>

 */
public class Ipx800Control
{
	protected int port = 9870;
	protected String server = "domus.bboett.lan";
	protected String returnMsg = "";

	/**
	setHost
	@argument the hostname to use, its a string, since i set up my own bind...
	*/
	public void setHost(String p0)
	{
		server = p0;
	}
	/**
	setPort
	@argument the port to use, in case you changed the default setting...
	*/
	public void setPort(int p0)
	{
		port = p0;
	}

	protected boolean sendCmd(String cmd)
	{
		Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
		//System.out.println("sendcmd for "+cmd);
        try
		{
			//System.out.println("opening "+server+":"+port);
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//System.out.println("about to send out cmd");
			out.println(cmd);
			//System.out.println("waiting for return");
			returnMsg = in.readLine();
			//System.out.println("return from ipx:"+returnMsg);
			if (returnMsg.matches("="))
			{
				String[] parts = returnMsg.split("=");
				//String part1 = parts[0]; // Getin
				returnMsg = parts[1]; // value
			}
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

	 @example Pour  mettre  le  relais  1  à  1  le  code  commande  est  Set011       

	 Pour  commander  la  sortie  en  mode  impulsionnel  il  faut  ajouter  le  
	 symbole  p  en  fin  de  chaine  et   avoir  été  définit  les  délais  
	 Ta  et  Tb  dans  l’interface  WEB  de  l’IPX800. 
	 Pour mettre le relais 1 à 1 en mode impulsionnel  le code commande est Set011p  
	 @param le no du relai a positionner (1-32)
	 @param vrai = actif, faux, ferme
	 */
	public boolean set(int relai, boolean etat)
	{return set(relai, etat, false);}
	public boolean set(int relai, boolean etat, boolean impulsion)
	{
		String cmd = "Set";
		if (relai < 1 || relai > 32) return false;
		if (relai < 10) cmd += "0"; 
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
	 @param vecteur de booleans (32),vrai = actif, faux, ferme

	 */
	public boolean bit(boolean[] relais)
	{
		if (relais == null || relais.length <= 0) return false;

		String cmd = "Bit=";
		for (int i = 0; i < Math.min(32, relais.length); i++)
			if (relais[i]) cmd += "1";
			else cmd += "0";

		return(sendCmd(cmd));
	}
	/*
	 Obtenir l‘état d’une entrée:  
	 GetIn Paramètres : GetInx ou x est ne numéro de l’entrée de 1 à 32 GetIn1 
	 permet d’obtenir l’état de l’entrée 1. 
	 L’IPX800 répond GetIn1=0  (Valeur  0 ou 1 en fonction de l’état de l‘entrée).
	 @param le no du relai a questionner (1-32)
	 @return l'etat du relai
	 */
	public boolean getIn(int relai)
	{
		if (relai < 1 || relai > 32) return false;

		String cmd = "GetIn" + relai;

		boolean retVal = false;

		if (sendCmd(cmd))
		{
			if (returnMsg.equals("1")) retVal = true;
		}

		return retVal;
	}
	/*

	 ••••                        Obtenir l‘état de toutes les entrées : 
	 GetInputs Paramètres : GetInputs permet de recevoir dans une seule trame
	 l’état des 32 entrées digitales. 
	 Réponse de l’IPX800: GetInputs=00000000000000000000000000000000 
	 (Le dernier caractère reçu correspond à  l’entrée 32)            
	 @return l'etat de tous les relais (32)
	 */
	public boolean[] getIn()
	{
		System.out.println("getIn");
		String cmd = "GetInputs";

		boolean[] retVal = new boolean[32];
		//System.out.println("getIn ckmmand to send "+cmd);
		if (sendCmd(cmd))
		{
			System.out.println("got return " + returnMsg);

			for (int i =0; i < 32;i++)
			{
				if (returnMsg.charAt(i) == '1') retVal[i] = true;
				else retVal[i] = false;
			}
		}
		else
		{
			for (int i =0; i < 32;i++) retVal[i] = false;
		}
		System.out.println("about to exit");
		return retVal;
	}
	/*

	 ••••                        Obtenir l‘état d’une entrée analogique :  
	 GetAn Paramètres : 
	 GetAnx ou x est le numéro de l’entrée analogique de 1 à 4 
	 GetAn1 permet de recevoir l’état de l’entrée analogique 1. 
	 @param le no (1-4) d'une des entrees analogiques
	 @return valeur de 0 à 1023           
	 */
	public int getAn(int entreeAnalogique)
	{
		if (entreeAnalogique < 1 || entreeAnalogique > 4) return -1;

		String cmd = "GetAn" + entreeAnalogique;

		int retVal = -1;

		if (sendCmd(cmd))
		{
			try
			{
				retVal = Integer.parseInt(returnMsg);
			}
			catch (NumberFormatException e)
			{
				System.err.println("no number: " + returnMsg);
			}
		}

		return retVal;
	}
	/*
	 Obtenir l‘état d’un compteur d’implusion :  
	 @example  GetCount(1) renvoi la valeur du compteur 1.
	 @param le numéro de compteur (de 1 à 3)
	 @return entier 32 Bits soit une valeur de 0 à 4294967295
	 */
	public int getCount(int counterId)
	{
		if (counterId < 1 || counterId > 3) return -1;

		String cmd = "GetCount" + counterId;

		int retVal = -1;

		if (sendCmd(cmd))
		{
			try
			{
				retVal = Integer.parseInt(returnMsg);
			}
			catch (NumberFormatException e)
			{
				System.err.println("no number: " + returnMsg);
			}
		}
		return retVal;
	}
	/*
	 Obtenir l‘état des compteurs d’implusion :  
	 @return vecteur d'entiers 32 Bits (soit 3x des valeurs de 0 à 4294967295)
	 */
	public int[] getCount()
	{
		int [] result = new int[3];
		for (int counterId = 1 ; counterId < 4; counterId++)
		{
			result[counterId-1] = getCount(counterId);			
		}

		return result;
	}

	/*
	 Obtenir l‘état d’une sortie:  
	 GetOut Paramètres : GetOutx ou x est le numéro de la sortie (de 1 à 32). 
	 @example GetOut1 renvoi la valeur de la sorite 1. 
	 @param le no de la sortie
	 @return Valeur 0 ou 1 en fonction de l’état de la sortie. 
	 */
	public boolean getOut(int relai)
	{
		if (relai < 1 || relai > 32) return false;

		String cmd = "GetOut" + relai;

		boolean retVal = false;

		if (sendCmd(cmd))
		{
			if (returnMsg.equals("1")) retVal = true;
		}

		return retVal;
	}
	/*
	 Obtenir l‘état de toute les  sorties:  
	 GetOutputs Paramètres : 
	 GetOutputs   permet de recevoir dans une seule trame l’état des 32 sorties. 
	 @return 00000000000000000000000000000000  
	 (Le dernier caractère reçu correspond à la sortie 32)            
	 */
	public boolean[] getOut()
	{
		String cmd = "GetOutputs";

		boolean[] retVal = new boolean[32];

		if (sendCmd(cmd))
		{
			for (int i =0; i < 32;i++)
			{
				if (returnMsg.charAt(i) == '1') retVal[i] = true;
				else retVal[i] = false;
			}
		}
		else
		{
			for (int i =0; i < 32;i++) retVal[i] = false;
		}

		return retVal;
	}
	/*	
	 Remise à zéro des compteurs:  
	 @param le numéro du compteur à remettre à zéro. 
	 @example ResetCount(1) remet le compteur 1 à zéro. 
	 @return vrai/faux                  
	 */
	public boolean resetCount(int relai)
	{
		if (relai < 1 || relai > 3) return false;

		String cmd = "ResetCount" + relai;

		boolean retVal = false;

		if (sendCmd(cmd) && returnMsg.equals("Success"))
			retVal = true;

		return retVal;
	}
	/*
	 Reset système de l’IPX800 
	 Reset L’envoi de cette commande provoque le redémarrage de l’IPX800 (Reboot). 
	 */
	public void reset()
	{
		String cmd = "Reset";
		sendCmd(cmd);
	}

	/*
	 Simuler une entrée  :  http://IPX800_V3/leds.cgi? 
	 Paramètres  : led=x où  x  est  le  numéro  de  l'entrée,  de  100  à  131. 
	 On  pourra  donc,  de  la  sorte,  commander  les  entrées  9  à  32  
	 (108  à  131  dans  la  commande  http)  même  si  les  extensions  
	 X880  ne  sont  pas présentes  physiquement.  
	 Pratique  pour  créer  des  assignations  virtuelles  ! •  

	 Réinitialiser  un  timer  :  http://IPX800_V3/protect/timers/timer1.htm? 
	 Paramètres  : erase=x où  x  est  le  numéro  du  timer  à  effacer,  
	 de  0  à  127. •  

	 Programmer un timer  :  http://IPX800_V3/protect/timers/timer1.htm? 
	 Paramètres  : timer=x  où  x  c'est  le  numéro  du  timer  concerné,  
	 de  0  à  127 day=x  où  x  est  le  jour  concerné  de  
	 0  à  6  (lundi  à  dimanche),  
	 7  pour  tous  les  jours,  
	 8  pour  les  jours  travaillés  (lundi  à  vendredi)  et  
	 9  pour  les  weekends. 
	 time=HH%3AMM  où  HH  représente  les  heures  et  MM  les  minutes  de  l'horaire 
	 choisi relay=x  où  x  est  le  numéro  de  sortie  assignée,  de  0  à  31,  
	 ou  de  compteur  assigné,  de  32  à  34. action=x  
	 où  x  est  le  numéro  d'action  
	 avec  0=off,  1=on,  2=inversion,  3=impulsion,  4=annulation  du  timer  
	 (valeur  vide)  et  7=pour  réinitialiser  les compteurs. •  

	 Commander une sortie  :  http://IPX800_V3/leds.cgi? 
	 Paramètre  : led=x avec  x  le  numéro  de  la  sortie,  de  0  à  31. 
	 Cette  syntaxe  permet  la  commande  directe  d'une  sortie.  
	 Cette  syntaxe  commandera  une  impulsion  si  la  sortie  concernée  a  été  
	 préréglée  avec  au moins  un  Tb  non  nul  dans  le  site  embarqué  de  
	 l'IPX.  Sinon  la  commande  inversera  tout  simplement  l'état  de  la  
	 sortie,  comme  un  télérupteur. 

	 •  Commander une sortie  sans  mode  impulsionnel  :  
	 http://IPX800_V3/preset.htm? 
	 Paramètre  : setx=1 ou  0  où  x le  numéro  de  la  sortie  de  1  à  32. 
	 Cette  syntaxe  permet  de  commander  un  état  de  sortie,  
	 c'est-à-dire  on  pour  1  ou  off  pour  0.  
	 Nous  avons  donc  là  une  sorte  d'interrupteur.  
	 Avantage de  cette  commande  :  elle  peut  tout  de  même  s'appliquer  à
	 une  sortie  préréglée  en  mode  impulsionnel.  
	 Par  conséquent,  pour  une  telle  sortie  un "led"  lancera  une  impulsion
	 alors  qu'un  "set"  forcera  un  état  on  ou  off  sans  impulsion. 

	 •  Gérer un compteur  et  sa  valeur  :  
	 http://IPX800_V3/protect/assignio/counter.htm? 
	 Paramètres  : counternamex=NOUVEAUNOM permet  de  renommer  le  
	 compteur  x,  de  1  à  3 counterx=123 permet  de  forcer  une  valeur  au
	 compteur  x Commande  très  pratique  pour  faire  une  remise  à  zéro  par
	 exemple. 

	 •  Gérer la configuration  d'une  sortie  :  
	 http://IPX800_V3/protect/settings/output1.htm? 
	 Paramètres  : output=x où  x  est  le  numéro  de  sortie  concernée,  
	 de  1  à  32 
	 */
}
