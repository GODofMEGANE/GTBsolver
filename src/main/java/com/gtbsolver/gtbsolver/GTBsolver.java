package com.gtbsolver.gtbsolver;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

@Mod(modid = GTBsolver.MODID, version = GTBsolver.VERSION)
public class GTBsolver
{
    final int SHOW_NUM = 30;

    public static final String MODID = "gtb_solver";
    public static final String VERSION = "1.0";
    public static final Logger logger = LogManager.getLogger(GTBsolver.MODID);
    
    public Scanner wordlistScanner;
    public Field hintField;
    public String beforeHint = "";
    public int suggestionNum = 0;
    ArrayList<String> wordlist = new ArrayList<String>();
    ArrayList<String> suggestionlist = new ArrayList<String>();
    ArrayList<String> answeredlist = new ArrayList<String>();

    @EventHandler
    public void init(FMLInitializationEvent event){
        try{
            wordlistScanner = new Scanner(new File("wordlist.txt"));
            GTBsolver.logger.info("wordlist.txt is loaded");
            while(wordlistScanner.hasNextLine()){
                wordlist.add(wordlistScanner.nextLine());
            }
        }catch(IOException e){
            System.err.println(e.getMessage());
            GTBsolver.logger.fatal("wordlist.txt is not found!");
            try {
                FileWriter fw = new FileWriter("wordlist.txt");
                fw.write("Please input answers list here!");
                fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if(event.type == RenderGameOverlayEvent.ElementType.ALL){
            FontRenderer fRender = Minecraft.getMinecraft().fontRendererObj;
            for(int i = 0;i < Math.min(suggestionlist.size(), SHOW_NUM);i++){
                fRender.drawStringWithShadow(suggestionlist.get(i), 5, 5+10*i, 16777215);
            }
            if(suggestionlist.size() > SHOW_NUM){
                fRender.drawStringWithShadow("and more...", 5, 5+10*SHOW_NUM, 11184810);
            }
        }
    }
    
    @SubscribeEvent
    public void onChat(final ClientChatReceivedEvent event){
        String[] chat = event.message.getUnformattedText().split(":");
        if(chat.length == 2){
            answeredlist.add(chat[1].trim().toLowerCase());
            for(int i = 0;i < suggestionlist.size(); i++){
                if(suggestionlist.get(i).trim().toLowerCase().equals(chat[1].trim().toLowerCase())){
                    suggestionlist.remove(i);
                }
            }
        }
    }

    @SubscribeEvent
    public void actionbar(TickEvent.PlayerTickEvent event){
        
        String hint = getCurrentActionBar();
        if(hint == null || hint.length() <= 17 || !hint.substring(2, 15).equals("The theme is ")){
            suggestionlist = new ArrayList<String>();
            return;
        }
        if(beforeHint.equals(hint.substring(17))){
            return;
        }
        hint = hint.substring(17);
        beforeHint = hint;
        suggestionlist = new ArrayList<String>();
        for(int i = 0; i < wordlist.size(); i++){
            if(hint.length() == wordlist.get(i).length()){
                boolean correct = true;
                for(int j = 0;j < hint.length(); j++){
                    if((hint.charAt(j) != '_' && hint.charAt(j) != wordlist.get(i).charAt(j)) || (hint.charAt(j) == '_' && wordlist.get(i).charAt(j) == ' ')){
                        correct = false;
                    }
                }
                if(correct){
                    suggestionlist.add(wordlist.get(i));
                }
            }
        }
        if(suggestionNum < suggestionlist.size()){
            answeredlist.clear();
        }
        for(int i = 0;i < suggestionlist.size(); i++){
            for(int j = 0;j < answeredlist.size(); j++){
                if(suggestionlist.get(i).trim().toLowerCase().equals(answeredlist.get(j))){
                    suggestionlist.remove(i);
                }
            }
        }
        suggestionNum = suggestionlist.size();
    }

    public static String getCurrentActionBar(){
		try {
			String actionBar = (String) ReflectionHelper.findField(GuiIngame.class, "recordPlaying", "field_73838_g").get(Minecraft.getMinecraft().ingameGUI);
			return actionBar;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}