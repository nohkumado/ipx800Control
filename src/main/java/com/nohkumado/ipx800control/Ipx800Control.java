/*

 Copyright (c) 2015 Bruno Boettcher. All rights reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This file is part of Foobar.

 IpxControl is free software: you can redistribute it and/or modify
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
import java.util.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
/**
 classe qui comprend un interfacage entre tous les accˋes TCP/I m2m vers un objet java
 Class that interfaces with the IPX800v3 through TCP/IP - m2m commands.

 @author Bruno Boettcher <nokumado@gmail.com>
  @since 10.2015

 */
public class Ipx800Control
{
  protected int port = 9870;
  protected String server = "domus.maison.home";
  protected String returnMsg = "";

  /**
   setHost
   @param p0 the hostname to use, its a string, since i set up my own bind...
   */
  public void setHost(String p0)
  {
	server = p0;
  }
  /**
   setPort
   @param p0 the port to use, in case you changed the default setting...
   */
  public void setPort(int p0)
  {
	port = p0;
  }
  /**
   sendCmd
   @param cmd the command to send
   opens a TCP port and sends a m2m command to the ipx, stores the eventual result in 
    return in @see returnMsg
   */
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
  /**
   sendHtmlCmd

   @param request a HttpGet Object with the GET encoded url and data
   @return the webpage fetched in a string
   */
  protected String sendHtmlCmd(String url)
  {
    String html = "";
    HttpGet request = null;
    try
    {
      request = new HttpGet(url);
    } 
    catch (java.net.URISyntaxException use)
    { System.err.println("problem with uri: " + use);}

    try
    {
      HttpClient client = new DefaultHttpClient();
      //HttpGet request = new HttpGet(url);
      HttpResponse response = client.execute(request);

      InputStream in = response.getEntity().getContent();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      StringBuilder str = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null)
      {
	str.append(line);
      }
      in.close();
      html = str.toString();
    }
    catch (IOException e)
    { System.err.println("problem: " + e);}
    catch (org.apache.http.HttpException he)
    { System.err.println("problem: " + he);}
    /*
    //TODO should put all time consuming stuff in a thread....
    Thread thread = new Thread(new Runnable(){
    @Override
    public void run()
    {
	//code to do the HTTP request
	}
	});
	thread.start();
     */
      return html;	  
  }
  /**
   Setxxy ou  xx  est  le  numéro  de  sortie  de  01  à  32 
   et  y  l’état  de  la  sortie    0  =  Off  et  1  =  On

   @example Pour  mettre  le  relais  1  à  1  le  code  commande  est  Set011       

    <p>
   Pour  commander  la  sortie  en  mode  impulsionnel  il  faut  ajouter  le  
   symbole  p  en  fin  de  chaine  et   avoir  été  définit  les  délais  
   Ta  et  Tb  dans  l’interface  WEB  de  l’IPX800. 
   Pour mettre le relais 1 à 1 en mode impulsionnel  le code commande est Set011p  
    </p>
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
  /** Commander les sorties  simultanément    :  
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
  /**
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
  /**
   Obtenir l‘état de toutes les entrées : 
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
  /**
   Obtenir l‘état d’une entrée analogique :  
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
  /**
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
  /**
   Obtenir l‘état des compteurs d’implusion :  
   @return vecteur d'entiers 32 Bits (soit 3x des valeurs de 0 à 4294967295)
   */
  public int[] getCount()
  {
	int [] result = new int[3];
	for (int counterId = 1 ; counterId < 4; counterId++)
	{
	  result[counterId - 1] = getCount(counterId);			
	}

	return result;
  }

  /**
   Obtenir l‘état d’une sortie:  
   GetOut Paramètres : GetOutx ou x est le numéro de la sortie (de 1 à 32). 
   @example GetOut1 renvoi la valeur de la sorite 1. 
   @param relai le no de la sortie
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
  /**
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
  /**	
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
  /**
   Reset système de l’IPX800 
   Reset L’envoi de cette commande provoque le redémarrage de l’IPX800 (Reboot). 
   */
  public void reset()
  {
	String cmd = "Reset";
	sendCmd(cmd);
  }

  /**
   Simuler une entrée  :  http://IPX800_V3/leds.cgi? 
   Paramètres  : led=x où  x  est  le  numéro  de  l'entrée,  de  100  à  131. 
   On  pourra  donc,  de  la  sorte,  commander  les  entrées  9  à  32  
   (108  à  131  dans  la  commande  http)  même  si  les  extensions  
   X880  ne  sont  pas présentes  physiquement.  
   Pratique  pour  créer  des  assignations  virtuelles  ! 
   @param no the led to light

   */
  public void setLed(int no)
  {
	String url = "http://" + server + "/leds.cgi";
	if (no < 100) no += 100;
	if (no >= 100 && no < 131)
	{
	  url += "?led=" + no;
	  sendHtmlCmd(url);
	}
  }

  /**
   Réinitialiser  un  timer  :  http://IPX800_V3/protect/timers/timer1.htm? 
   Paramètres  : erase=x où  x  est  le  numéro  du  timer  à  effacer,  
   de  0  à  127. 
   @param no the number of the timer to erase
   */
  public void eraseTimer(int no)
  {
	String url = "http://" + server + "/protect/timers/timer1.htm?";
	if (no >= 0 && no < 127)
	{
	  url += "erase=" + no;
	  sendHtmlCmd(url);
	}
  }
  /** Programmer un timer  :  http://IPX800_V3/protect/timers/timer1.htm? 
   Paramètres  : 
   @param no timer=x  où  x  c'est  le  numéro  du  timer  concerné,  de  0  à  127 
   @param day day=x  où  x  est  le  jour  concerné  de  
   0  à  6  (lundi  à  dimanche),  
   7  pour  tous  les  jours,  
   8  pour  les  jours  travaillés  (lundi  à  vendredi)  et  
   9  pour  les  weekends. 
   @param time time=HH%3AMM  où  HH  représente  les  heures  et  MM  les  minutes  de  l'horaire choisi 
   @param relai relay=x  où  x  est  le  numéro  de  sortie  assignée,  de  0  à  31,  
   ou  de  compteur  assigné,  de  32  à  34. 
   @param action action=x où  x  est  le  numéro  d'action  
   avec  0=off,  1=on,  2=inversion,  3=impulsion,  4=annulation  du  timer  
   (valeur  vide)  et  7=pour  réinitialiser  les compteurs. •  
   */
  public boolean programTimer(int no, int day, String time, int relai, int action)
  {
	boolean result = true;
	String url = "http://" + server + "/protect/timers/timer1.htm?";
	if (no < 0 || no > 127) result = false;
	if (day < 0 || day > 9) result = false;
	if (!time.matches("\\d{2}%3A\\d{2}")) result = false;
	if (relai < 0 || relai > 34) result = false;
	if (action < 0 || action > 7) result = false;
	if (result)
	{
	  url += "timer=" + no + "&day=" + day + "&time=" + time + "&relay=" + relai + "&action=" + action;
	  sendHtmlCmd(url);
	}
	return result;
  }
  /**
   Commander une sortie  :  http://IPX800_V3/leds.cgi? 
   Paramètre  : led=x avec  x  le  numéro  de  la  sortie,  de  0  à  31. 
   Cette  syntaxe  permet  la  commande  directe  d'une  sortie.  
   Cette  syntaxe  commandera  une  impulsion  si  la  sortie  concernée  a  été  
   préréglée  avec  au moins  un  Tb  non  nul  dans  le  site  embarqué  de  
   l'IPX.  Sinon  la  commande  inversera  tout  simplement  l'état  de  la  
   sortie,  comme  un  télérupteur.
   @param no the relai to trigger   
   */
  public void activateRelai(int no)
  {
	String url = "http://" + server + "/leds.cgi";
	if (no >= 0 && no < 32)
	{
	  url += "?led=" + no;
	  sendHtmlCmd(url);
	}
  }
  /** •  Commander une sortie  sans  mode  impulsionnel  :  
   http://IPX800_V3/preset.htm? 
   Paramètre  : setx=1 ou  0  où  x le  numéro  de  la  sortie  de  1  à  32. 
   Cette  syntaxe  permet  de  commander  un  état  de  sortie,  
   c'est-à-dire  on  pour  1  ou  off  pour  0.  
   Nous  avons  donc  là  une  sorte  d'interrupteur.  
   Avantage de  cette  commande  :  elle  peut  tout  de  même  s'appliquer  à
   une  sortie  préréglée  en  mode  impulsionnel.  
   Par  conséquent,  pour  une  telle  sortie  un "led"  lancera  une  impulsion
   alors  qu'un  "set"  forcera  un  état  on  ou  off  sans  impulsion.
   @param no the relai to set
   @param state true for on false for off
   */
  public void setRelai(int no, boolean state)
  {
	String url = "http://" + server + "/preset.htm?set";
	if (no >= 0 && no < 32)
	{
	  url += "" + no + "=";
	  if (state) url += "1"; else url += "0";
	  sendHtmlCmd(url);
	}
  }

  /**
   Gérer un compteur  et  sa  valeur  :  
   http://IPX800_V3/protect/assignio/counter.htm? 
   Paramètres  : counternamex=NOUVEAUNOM permet  de  renommer  le  
   compteur  x,  de  1  à  3 counterx=123 permet  de  forcer  une  valeur  au
   compteur  x Commande  très  pratique  pour  faire  une  remise  à  zéro  par
   exemple. 
   */
  public void counter(int no, String name, int value)
  {
	String url = "http://" + server + "/protect/assignio/counter.htm?";
	if (no >= 0 && no < 4)
	{
	  if (name != null)
	  {
		url += "countername" + no + "=" + name;
		if (value >= 0) url += "&"; 
	  }
	  if (value >= 0) url += "counter" + no + "=" + value;
	  sendHtmlCmd(url);
	}
  }
  public void setCounterName(int no, String name)
  {counter(no, name, -1);}
  public void counter(int no, int value)
  {counter(no, null, value);}

  /**Gérer la configuration  d'une  sortie  :  
   http://IPX800_V3/protect/settings/output1.htm? 
   Paramètres  : output=x où  x  est  le  numéro  de  sortie  concernée,  de  1  à  32 
   relayname=LumiereTerrasse avec  ce  paramètre  on  peut  modifier  le  nom  de  la  sortie 
   delayon=x il  s'agit  là  du  Ta  où  x  exprime,  en  dixième  de  seconde,  
   le  temps  de  retard  avant  mise  à  on,  valeur  max  65535  soit  un  peu  plus  de  1h49  
   delayoff=y  le  Tb  où  y,  en  dixième  de  seconde, le  temps  de  maintien  avant  
   remise  à  off  de  l'impulsion,  valeur  max  idem 
   Ici  nous  pouvons  modifier  "à  la  volée"  n'importe  quelle  configuration  de
   sortie  :  
   son  nom  et  même  le  Ta  et  le  Tb,  
   on  peut  donc  rendre  une  sortie impulsionnelle  ou  à  l'inverse  arrêter  le  
   mode  impulsionnel  en  remettant  un  Ta  et  un  Tb  nuls. 
   @param no the relai to configure
   @param name the name to give the relai
   @param delayon in 0,1s steps the time for the delai before switching on
   @param delayoff in 0,1s steps the time for the delai before switching on
   */
  public boolean confOut(int no, String name, int delayon, int delayoff)
  {
	boolean result = true;
	String url = "http://" + server + "/protect/settings/output1.htm?";
	if (no < 0 || no > 32) result = false;
	if (result)
	{
	  url += "output=" + no + "&relayname=" + name + "&delayon=" + delayon + "&delayoff=" + delayoff;
	  sendHtmlCmd(url);
	}
	return result;
  }
  /**
   Gérer la configuration  d'une  entrée  numérique  :  
   http://IPX800_V3/protect/assignio/assign1.htm? 
   Paramètres  : input=x où  x  est  le  numéro  d'entrée  concernée,  de  0à  31 
   inputname=Inter1 avec  ce  paramètre  on  peut  modifier  le  nom  de  
   l'entrée lx=1 ce  petit  L  permet  de  choisir  une  sortie  assignée  
   (x  de  0  à  31) 
   mode=x là  c'est  le  mode  d'assignation  où  
   x  =  0  pour  on/off,  
   1  pour  switch,  
   2  pour  VR,  
   3  pour  on  et  
   4  pour  off. 
   inv=1 si  nécessaire,  permet  d'inverser  la  logique  d'entrée.

   */
  /**
   Gérer la configuration  d'une  entrée  analogique  :  
   http://IPX800_V3/protect/assignio/analog1.htm? 
   Paramètres  : 
   analog=x où  x  est  le  numéro  de  l'entrée  concernée,  de  0  à  3 
   name=Temperature permet  de  renommer  l'entrée 
   selectinput=4 permet  de  choisir  un  type  de  capteur  avec  
   0=valeur  brute,  1=tension,  2=TC4012,  3=SHT-X3  lumière,  
   4=SHT-X3  température  et 5=SHT-X3  humidité 
   hi=x où  x  est  la  valeur  brute  de  seuil  haut 
   mhi=0 ou  1  pour  off  ou  on,  il  s'agit  là  de  l'action  sur  la  
   ou  les  sortie(s)  assignée(s),  paramètres  lka   
   lo=696 mlo=0 lkax=1 ou  1  pour  off  ou  on,  il  s'agit  de  l'action  
   sur  la  ou  les  sortie(s)  assignée(s) permet  de  choisir  une  sortie
   assignée  (x  de  1  à  8)

   */

  /**
   Programmer le ping  watchdog  :  
   http://IPX800_V3/protect/settings/ping.htm? 
   Paramètres  : pingip=xxx.xxx.xxx.xxx permet  de  choisir  l'adresse  IP  
   à  "pinguer" 
   pingtime=x 
   pingretry=x 
   prelay=x où  x  est  le  nombre  de  secondes  pour  l'intervalle  des  
   tentatives  de  ping où  x  est  le  nombre  d'essais  de  ping  avant  
   commande  de  la  sortie  choisie où x est la sortie assignée de 0 à 31 

   */

  /**
   Formulaire  XML Un formulaire  XML  vous  permet  de  récupérer  l’état  
   des  entrées/sorties  de  l’IPX800 Celui-ci  est  disponible  à      
   l’adresse      
   http://ipx800_v3/status.xml 

   API IPX800 -  GCE Electronics - copyright 2010-2013 
   */
  public String status()
  {
	String url = "http://" + server + "/status.xml";
	  return sendHtmlCmd(url);
	  /*
	<response>
	  <led0>0</led0><led1>0</led1><led2>0</led2><led3>0</led3><led4>0</led4><led5>0</led5>
	  <led6>0</led6><led7>0</led7><led8>0</led8><led9>0</led9><led10>0</led10>
	  <led11>0</led11><led12>0</led12><led13>0</led13><led14>0</led14><led15>0</led15>
	  <led16>0</led16><led17>0</led17><led18>0</led18><led19>0</led19><led20>0</led20>
	  <led21>0</led21><led22>0</led22><led23>0</led23><led24>0</led24><led25>0</led25>
	  <led26>0</led26><led27>0</led27><led28>0</led28><led29>0</led29><led30>0</led30>
	  <led31>0</led31>
	  
	  <btn0>up</btn0><btn1>up</btn1><btn2>up</btn2><btn3>up</btn3><btn4>up</btn4>
	  <btn5>up</btn5><btn6>up</btn6><btn7>up</btn7><btn8>up</btn8><btn9>up</btn9>
	  <btn10>up</btn10><btn11>up</btn11><btn12>up</btn12><btn13>up</btn13><btn14>up</btn14>
	  <btn15>up</btn15><btn16>up</btn16><btn17>up</btn17><btn18>up</btn18><btn19>up</btn19>
	  <btn20>up</btn20><btn21>up</btn21><btn22>up</btn22><btn23>up</btn23><btn24>up</btn24>
	  <btn25>up</btn25><btn26>up</btn26><btn27>up</btn27><btn28>up</btn28><btn29>up</btn29>
	  <btn30>up</btn30><btn31>up</btn31>
	  
	  <day>16/10/2015 </day>
	  <time0>19:30:59</time0> 
	  <analog0>0</analog0><analog1>0</analog1><analog2>0</analog2><analog3>0</analog3>
	  <analog4>0</analog4><analog5>0</analog5><analog6>0</analog6><analog7>0</analog7>
	  <analog8>0</analog8><analog9>0</analog9><analog10>0</analog10><analog11>0</analog11>
	  <analog12>0</analog12><analog13>0</analog13><analog14>0</analog14>
	  <analog15>0</analog15>
	  
	  <anselect0>0</anselect0><anselect1>0</anselect1><anselect2>0</anselect2>
	  <anselect3>0</anselect3><anselect4>0</anselect4><anselect5>0</anselect5>
	  <anselect6>0</anselect6><anselect7>0</anselect7><anselect8>0</anselect8>
	  <anselect9>0</anselect9><anselect10>0</anselect10><anselect11>0</anselect11>
	  <anselect12>0</anselect12><anselect13>0</anselect13><anselect14>0</anselect14>
	  <anselect15>0</anselect15>
	  
	  <count0>23</count0> <count1>0</count1> <count2>0</count2> <count3>0</count3> 
	  <count4>0</count4> <count5>0</count5> <count6>0</count6> <count7>0</count7>
	  
	  <tinfo>---</tinfo>
	  <version>3.05.59d</version>
</response>




 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[421,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[438,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[469,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[490,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[514,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[538,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[573,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown
 [ERROR] /home/bboett/java/ipx800Control/src/main/java/com/nohkumado/ipx800control/Ipx800Control.java:[636,30] unreported exception java.net.URISyntaxException; must be caught or declared to be thrown




	  */
  }
  
}
