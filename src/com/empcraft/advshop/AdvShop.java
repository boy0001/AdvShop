package com.empcraft.advshop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;




public final class AdvShop extends JavaPlugin implements Listener {
	private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    private static YamlConfiguration idlist;
    private static YamlConfiguration prices;
    private static File idlistFile;
    private static File pricesFile;
    private static File historyFile;
    AdvShop plugin;
    String version;
	private ScriptEngine engine = (new ScriptEngineManager()).getEngineByName("JavaScript");
	
	// RETURNS ITEMSTACK | FRIENDLY NAME
	
	public String concatDouble(String value) {
		if (value.contains(".")) {
			try {
				return value.split("\\.")[0]+"."+value.split("\\.")[1].substring(0, 2);
			}
			catch (Exception e) {
				return value;
			}
		}
		else {
			return value;
		}
	}
	
	
    private Object[] LevensteinDistance(String string) {
	 	Object[] toreturn = new Object[3];
	 	try {
	 		toreturn[0] = new ItemStack(Integer.parseInt(string));
	 		toreturn[1] = Material.getMaterial(Integer.parseInt(string)).toString();
	 		toreturn[2] = -4;
    		return  toreturn;
    	}
    	catch (Exception e) {
    		
    	}
    	string = StringUtils.replace(string.toString(), " ", "_").toUpperCase();
    	Material[] materials = Material.values();
		int smallest = -100;
		String materialname = null;
    	ItemStack lastmaterial = null;
		Set<String> ids = idlist.getConfigurationSection("item-ids").getKeys(false);
		try {
			string = string.replace(":","-");
			toreturn[1] = ""+split(idlist.getString("item-ids."+string))[0];
			toreturn[0] = new ItemStack(Integer.parseInt(string.split("-")[0]),1, Short.parseShort(string.split("-")[1]));
			toreturn[2] = -4;
			return toreturn;
		}
		catch (Exception e) {
			if(string.contains("-")) {
				string = string.replace("-", ":");
			}
			if (string.contains(":")) {
				try {
					toreturn[1] = Integer.parseInt(string.split(":")[0])+":"+Integer.parseInt(string.split(":")[1]);
					toreturn[0] = new ItemStack(Integer.parseInt(string.split(":")[0]),1, Short.parseShort(string.split(":")[1]));
					toreturn[2] = -4;
					return toreturn;
				}
				catch (Exception e2) {
				}
			}
		}
		for (String current:ids) {
			for (String itemname:split(idlist.getString("item-ids."+current))) {
				if (smallest == -100) {
					lastmaterial = new ItemStack(Material.AIR);
					smallest = 100;
				}
				else {
					int distance = StringUtils.getLevenshteinDistance(string.toUpperCase(), itemname.toUpperCase());
					if (itemname.contains(string)) {
						distance = StringUtils.getLevenshteinDistance(string.toUpperCase(), itemname.toUpperCase())-4;
						if (distance<smallest) {
							if (current.contains("-")) {
								lastmaterial = new ItemStack(Integer.parseInt(current.split("-")[0]),1, Short.parseShort(current.split("-")[1]));
							}
							else {
								lastmaterial = new ItemStack(Integer.parseInt(current),1, Short.parseShort("0"));
							}
							smallest = distance;
							materialname=itemname;
						}
					}
					else {
						if (distance<smallest) {
							if (current.contains("-")) {
								lastmaterial = new ItemStack(Integer.parseInt(current.split("-")[0]),1, Short.parseShort(current.split("-")[1]));
							}
							else {
								lastmaterial = new ItemStack(Integer.parseInt(current),1, Short.parseShort("0"));
							}
							materialname=itemname;
							smallest = distance;
						}
					}
				}
			}
		}
		boolean subtract = false;
    	for (Material mymaterial:materials) {
    		String current = mymaterial.toString();
    		if (smallest == -1) {
    			lastmaterial = new ItemStack(mymaterial);
    			materialname=mymaterial.toString();
    			smallest = 100;
    		}
    		else {
    			boolean tosubtract = false;
    			int distance;
    			if (current.contains(string)) {
    				tosubtract = true;
    				distance = StringUtils.getLevenshteinDistance(string.toUpperCase(), current)-4;
    				if (distance==-1) {
    					distance = 0;
    				}
    			}
    			else {
    				distance = StringUtils.getLevenshteinDistance(string.toUpperCase(), current)+Math.abs(string.length()-current.length());
    			}
    			if (distance<smallest) {
    				if (tosubtract) {
    					subtract = true;
    				}
    				else {
    					subtract = false;
    				}
    				materialname=mymaterial.toString();
    				smallest = distance;
    				lastmaterial = new ItemStack(mymaterial);
    			}
    		}
    	}
    	toreturn[0] = lastmaterial;
    	materialname.replace(" ", "_");
    	toreturn[1] = materialname;
    	if (subtract) {
    		smallest+=4;
    	}
    	toreturn[2] = smallest;
    	return toreturn;
    }
	public Double javascript(String line) {
        try {
        	Object toreturn;
        	if ((line.contains(".js"))&&(line.contains(" ")==false)) {
        		File file = new File(getDataFolder() + File.separator + "scripts" + File.separator + line);
        		return (Double) engine.eval(new java.io.FileReader(file));
        	}
        	else {
        		return (Double) engine.eval(line);
        	}
		} catch (Exception e) { }
        return null;
	}
	public String getmsg(String key) {
		File yamlFile = new File(getDataFolder(), getConfig().getString("language").toLowerCase()+".yml"); 
		YamlConfiguration.loadConfiguration(yamlFile);
		try {
			return colorise(YamlConfiguration.loadConfiguration(yamlFile).getString(key));
		}
		catch (Exception e){
			return "";
		}
	}
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    public String colorise(String mystring) {
    	String[] codes = {"&1","&2","&3","&4","&5","&6","&7","&8","&9","&0","&a","&b","&c","&d","&e","&f","&r","&l","&m","&n","&o","&k"};
    	for (String code:codes) {
    		mystring = mystring.replace(code, "§"+code.charAt(1));
    	}
    	return mystring;
    }
    public boolean checkperm(Player player,String perm) {
    	boolean hasperm = false;
    	String[] nodes = perm.split("\\.");
    	
    	String n2 = "";
    	if (player==null) {
    		return true;
    	}
    	else if (player.hasPermission(perm)) {
    		hasperm = true;
    	}
    	else if (player.isOp()==true) {
    		hasperm = true;
    	}
    	else {
    		for(int i = 0; i < nodes.length-1; i++) {
    			n2+=nodes[i]+".";
            	if (player.hasPermission(n2+"*")) {
            		hasperm = true;
            	}
    		}
    	}
		return hasperm;
    }
    public void msg(Player player,String mystring) {
    	mystring = mystring.replace("{PLUGIN}", "AdvShop").replace("{VERSION}", version).replace("{AUTHOR}", "Empire92").replace("{BREAK}", "\n");
    	if (mystring.equals("")) { return; }
    	if (player==null) {
    		getServer().getConsoleSender().sendMessage(colorise(mystring));
    	}
    	else if (player instanceof Player==false) {
    		getServer().getConsoleSender().sendMessage(colorise(mystring));
    	}
    	else {
    		player.sendMessage(colorise(mystring));
    	}

    }
    public String matchgroup(String group) {
		String[] groups = (perms.getGroups());
		for (String current:groups) {
			if (group.equalsIgnoreCase(current)) {
				return current;
			}
		}
		return "";
    }
	public void onDisable() {
    	try {
        	timer.cancel();
        	timer.purge();
    	}
    	catch (IllegalStateException e) {
    		
    	}
    	catch (Throwable e) {
    		
    	}
	    	reloadConfig();
	    	saveConfig();
			msg(null,getmsg("INFO1"));
			msg(null,getmsg("INFO2"));
    }
	public boolean addLastLine(File fileName,String line) throws IOException {
		if (getConfig().getInt("keep-history-days")==0) {
			return false;
		}
		RandomAccessFile myFile = new RandomAccessFile(fileName, "rw");
		// Set write pointer to the end of the file
		myFile.seek(myFile.length());
		myFile.writeBytes(line+"\n");
		myFile.close();
		return true;
	}
	
	public boolean removeFirstLine(File fileName) throws IOException {
		String line = null;
		try {
			boolean toreturn = true;
		try {
			FileReader namereader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(namereader);
			line = br.readLine();
			Long time = System.currentTimeMillis()/1000L;
			String[] mysplit = line.split(" - ");
			Long filetime;
			try {
				filetime = Long.parseLong(mysplit[0]);
			}
			catch (Exception e3) {
				filetime = Long.parseLong(mysplit[0].replaceAll("[^0-9]", ""));
			}
			Long keep = ((86400L*getConfig().getLong("keep-history-days")));
			if (getConfig().getInt("keep-history-days")<1) {
				return false;
			}
			if (filetime+keep>time) {
				return false;
			}
			else {
			}
			// 51232132321 - 3/5/12 - 05:15 - Empire92 - BUY - 100 - GRASS - 2 - $100
			//     0            1       2         3       4     5      6      7      8
			String name = mysplit[7];
			int amount = Integer.parseInt(mysplit[5]);
			if (mysplit[4].equalsIgnoreCase("buy")) {
				prices.set(name+".bought", prices.getInt(name+".bought")-amount);
				prices.set("total.bought", prices.getInt("total.bought")-amount);
			}
			else {
				prices.set(name+".sold", prices.getInt(name+".sold")-amount);
				prices.set("total.sold", prices.getInt("total.sold")-amount);
			}
			}
			catch (Exception e) {
				if (line==null) {
					toreturn = false;
				}
				else {
					e.printStackTrace();
				}
			}
			//DONE READING FIRST LINE
		    RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
		     //Initial write position                                             
		    long writePosition = raf.getFilePointer();                            
		    raf.readLine();                                                       
		    // Shift the next lines upwards.                                      
		    long readPosition = raf.getFilePointer();                             
	
		    byte[] buff = new byte[1024];                                         
		    int n;                                                                
		    while (-1 != (n = raf.read(buff))) {                                  
		        raf.seek(writePosition);                                          
		        raf.write(buff, 0, n);                                            
		        readPosition += n;                 
		        writePosition += n;                                               
		        raf.seek(readPosition);                                           
		    }                                                                     
		    raf.setLength(writePosition);                                         
		    raf.close();
		    return toreturn;
		}
		catch (Exception e2) {
			return false;
		}
	}
	
    Timer timer = new Timer ();
    
	TimerTask mytask = new TimerTask () {
		@Override
	    public void run () {
			try {
				boolean result = true;
				int tosend = 0;
				while (result) {
					result = removeFirstLine(historyFile);
					if (result) { tosend+=1; }
				}
				if (tosend>0) {
					msg(null,getmsg("PURGE1").replace("{INT}",tosend+""));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	
	@Override
	public void onEnable(){
		plugin = this;
		version = "0.2.0";
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveResource("english.yml", true);
        File myfile = new File(getDataFolder() +File.separator+"idlist.yml");
        if(myfile.exists()!=true) {  saveResource("idlist.yml", false); }
        myfile = new File(getDataFolder() +File.separator+"history.yml");
        if(myfile.exists()!=true) {  saveResource("history.yml", false); }
        myfile = new File(getDataFolder() + File.separator+"prices.yml");
        if(myfile.exists()!=true) {  saveResource("prices.yml", false); }
        getConfig().options().copyDefaults(true);
        final Map<String, Object> options = new HashMap<String, Object>();
        getConfig().set("version", version);
        options.put("item-similarity-match", 1);
        options.put("default.bought", 128);
        options.put("cross_map_trade", 128);
        options.put("default.sold", 128);
        options.put("fomula.buy", "((2*bought*totalvolume)/(volume*volume))");
        options.put("fomula.sell", "((bought*totalvolume)/(volume*volume))");
        options.put("language","english");
        options.put("require-sold",true);
        options.put("use-exp-instead",false);
        options.put("require-stock-to-buy",false);
        options.put("keep-history-days",30);
        options.put("min-sell-price",0);
        options.put("idlist","idlist");
        for (final Entry<String, Object> node : options.entrySet()) {
        	 if (!getConfig().contains(node.getKey())) {
        		 getConfig().set(node.getKey(), node.getValue());
        	 }
        }
    	saveConfig();
    	saveDefaultConfig();
    	Bukkit.getServer().getPluginManager().registerEvents(this, this);
    	
    	idlistFile = new File(getDataFolder()+File.separator+getConfig().getString("idlist")+".yml");
		idlist = YamlConfiguration.loadConfiguration(idlistFile);
		historyFile = new File(getDataFolder()+File.separator+"history.yml");
		pricesFile = new File(getDataFolder()+File.separator+"prices.yml");
		prices = YamlConfiguration.loadConfiguration(pricesFile);
		timer.schedule (mytask,0l, 60000);
	}
	@EventHandler
	public void onSignChange(SignChangeEvent event)
    {
		Block block = event.getBlock();
		Sign sign = (Sign)block.getState();
		String line1 = event.getLine(0);
		String line2 = event.getLine(1);
		Player player = event.getPlayer();
		if (ChatColor.stripColor(line1).equalsIgnoreCase(ChatColor.stripColor(getmsg("SIGNSUCCESS0")))) {
			if (checkperm(player,"advshop.create.buy")) {
				Object[] result = LevensteinDistance(line2);
				String name = result[1]+"";
				if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
					ItemStack item = (ItemStack) result[0];
					if (name.length()>15) {
						name = item.getTypeId()+":"+item.getAmount();
					}
					sign.setLine(1, name);
					sign.update(true);
					msg(player,getmsg("INFO15"));
				}
				else {
					sign.setLine(0,getmsg("SIGNERROR0"));
					sign.update(true);
					msg(player,getmsg("SEARCH1").replace("{STRING}",line2.toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
				}
			}
			else {
				sign.setLine(0,getmsg("SIGNERROR0"));
				sign.update(true);
				msg(player,getmsg("ERROR0").replace("{STRING}","advshop.create.buy"));
			}
		}
		else if (ChatColor.stripColor(line1).equalsIgnoreCase(ChatColor.stripColor(getmsg("SIGNSUCCESS1")))) {
			if (checkperm(player,"advshop.create.sell")) {
				Object[] result = LevensteinDistance(line2);
				String name = result[1]+"";
				if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
					ItemStack item = (ItemStack) result[0];
					if (name.length()>15) {
						name = item.getTypeId()+":"+item.getAmount();
					}
					sign.setLine(1, name);
					sign.update(true);
					msg(player,getmsg("INFO15"));
				}
				else {
					sign.setLine(0,getmsg("SIGNERROR1"));
					sign.update(true);
					msg(player,getmsg("SEARCH1").replace("{STRING}",line2.toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
				}
			}
			else {
				sign.setLine(0,getmsg("SIGNERROR1"));
				sign.update(true);
				msg(player,getmsg("ERROR0").replace("{STRING}","advshop.create.sell"));
			}
		}
    }
	public int getVolume(ItemStack item) {
		int id = item.getTypeId();
		int damage = item.getDurability();
		if (damage!=0) {
			return prices.getInt(id+"-"+damage+".volume");
		}
		else {
			return prices.getInt(id+".volume");
		}
	}
	
	public int getBought(ItemStack item) {
		int id = item.getTypeId();
		int damage = item.getDurability();
		if (damage!=0) {
			return prices.getInt(id+"-"+damage+".bought");
		}
		else {
			return prices.getInt(id+".bought");
		}
	}
	
	public int getTotalBought() {
		return prices.getInt("total.bought");
	}
	
	public int getTotalSold() {
		return prices.getInt("total.sold");
	}
	
	public int getSold(ItemStack item) {
		int id = item.getTypeId();
		int damage = item.getDurability();
		if (damage!=0) {
			return prices.getInt(id+"-"+damage+".sold");
		}
		else {
			return prices.getInt(id+".sold");
		}
	}
	public double getBuyValue(ItemStack item) {
		String toeval = getConfig().getString("fomula.buy");
		try {
			if (toeval.contains("totalbought")) {
				toeval = toeval.replace("totalbought", ""+getTotalBought());
			}
			if (toeval.contains("totalsold")) {
				toeval = toeval.replace("totalsold", ""+getTotalSold());
			}
			if (toeval.contains("bought")) {
				toeval = toeval.replace("bought", ""+getBought(item));
			}
			if (toeval.contains("sold")) {
				toeval = toeval.replace("sold", ""+getSold(item));
			}
			if (toeval.contains("totalvolume")) {
				toeval = toeval.replace("totalvolume", ""+(getTotalBought()+getTotalSold()));
			}
			if (toeval.contains("volume")) {
				toeval = toeval.replace("volume", ""+(getBought(item)+getSold(item)));
			}
			Double result = javascript(toeval);
			if (result.equals(null)) {
				return 0;
			}
			else if (result.equals(Double.NaN)) {
				return 0;
			}
			else {
				return result;
			}
			}
		catch (Exception e) {
			msg(null,getmsg("ERROR1").replace("{STRING}",toeval+""));
		}
		return 0;
	}
	
	public double getSellValue(ItemStack item) {
		String toeval = getConfig().getString("fomula.sell");
		try {
			if (toeval.contains("totalbought")) {
				toeval = toeval.replace("totalbought", ""+getTotalBought());
			}
			if (toeval.contains("totalsold")) {
				toeval = toeval.replace("totalsold", ""+getTotalSold());
			}
			if (toeval.contains("bought")) {
				toeval = toeval.replace("bought", ""+getBought(item));
			}
			if (toeval.contains("sold")) {
				toeval = toeval.replace("sold", ""+getSold(item));
			}
			if (toeval.contains("totalvolume")) {
				toeval = toeval.replace("totalvolume", ""+(getTotalBought()+getTotalSold()));
			}
			if (toeval.contains("volume")) {
				toeval = toeval.replace("volume", ""+(getBought(item)+getSold(item)));
			}
			Double result = javascript(toeval);
			if (result.equals(null)) {
				return getConfig().getDouble("min-sell-price");
			}
			else if (result.equals(Double.NaN)) {
				return getConfig().getDouble("min-sell-price");
			}
			else {
				return result;
			}
		}
		catch (Exception e) {
			msg(null,getmsg("ERROR2").replace("{STRING}",toeval+""));
		}
		return 0;
	}
	boolean setSold(ItemStack item,int amount) {
		String idname = ""+item.getTypeId();
		if (item.getDurability()!=0) {
			idname+="-"+item.getDurability();
		}
		if (idlist.contains("item-ids."+idname)) {
			int total = 0;
			int current = 0;
			try { total = prices.getInt("total.sold");current = prices.getInt(idname+".sold"); } catch (Exception e) {};
			prices.set("total.sold", total+(amount-current));
			prices.set(idname+".sold",amount);
			return true;
		}
		return false;
	}
	boolean setBought(ItemStack item,int amount) {
		String idname = ""+item.getTypeId();
		if (item.getDurability()!=0) {
			idname+="-"+item.getDurability();
		}
		if (idlist.contains("item-ids."+idname)) {
			int total = 0;
			int current = 0;
			try { total = prices.getInt("total.bought");current = prices.getInt(idname+".bought"); } catch (Exception e) {};
			prices.set("total.bought", total+(amount-current));
			prices.set(idname+".bought",amount);
			return true;
		}
		return false;
	}
	
	
	public boolean Sell(ItemStack item,Player player) {
		String idname = ""+item.getTypeId();
		if (item.getDurability()!=0) {
			idname+="-"+item.getDurability();
		}
		if (idlist.contains("item-ids."+idname)) {
			if (prices.contains("total.sold")) {
				try {
					prices.set("total.sold", prices.getInt("total.sold")+item.getAmount());
				}
				catch (Exception e) {
					prices.set("total.sold", item.getAmount()+getConfig().getInt("default.sold"));
					prices.set("total.bought", getConfig().getInt("default.bought"));
				}
			}
			else {
				prices.set("total.sold", item.getAmount()+getConfig().getInt("default.sold"));
				prices.set("total.bought", getConfig().getInt("default.bought"));
			}
			if (prices.contains(idname+".sold")) {
				try {
					prices.set(idname+".sold", prices.getInt(idname+".sold")+item.getAmount());
				}
				catch (Exception e) {
					prices.set(idname+".sold", item.getAmount()+getConfig().getInt("default.sold"));
					prices.set(idname+".bought", getConfig().getInt("default.bought"));
				}
			}
			else {
				prices.set(idname+".sold", item.getAmount()+getConfig().getInt("default.sold"));
				prices.set(idname+".bought", getConfig().getInt("default.bought"));
			}
			return true;
		}
		else {
		}
		return false;
	}
	public boolean Buy(ItemStack item,Player player) {
		String idname = ""+item.getTypeId();
		if (item.getDurability()!=0) {
			idname+="-"+item.getDurability();
		}
		if (idlist.contains("item-ids."+idname)) {
//	        options.put("require-sold",true);
//	        options.put("require-stock-to-buy",true);
			if (prices.contains(idname+".bought")) {
				boolean hasstock = true;
				if (getConfig().getBoolean("require-stock-to-buy")) {
					if (prices.getInt(idname+".bought")>=prices.getInt(idname+".sold")) {
						hasstock = false;
					}
				}
				if (hasstock) {
					prices.set(idname+".bought", prices.getInt(idname+".bought")+item.getAmount());
					prices.set("total.bought", prices.getInt("total.bought")+item.getAmount());
					return true;
				}
				else {
					return false;
				}
			}
			else {
				if (getConfig().getBoolean("require-stock-to-buy")==false) {
					if (getConfig().getBoolean("require-stock-to-sold")==false) {
						prices.set(idname+".bought", getConfig().getInt("default.bought")+item.getAmount());
						prices.set(idname+".sold", getConfig().getInt("default.sold"));
						try {
							prices.set("total.bought", prices.getInt("total.bought")+item.getAmount());
						}
						catch (Exception e) {
							prices.set("total.bought",getConfig().getInt("default.bought")+item.getAmount());
						}
						return true;
					}
				}
			}
		}
		else {
		}
		return false;
	}
	
	private String[] split(String string) {
		if (string.contains(",")) {
			return string.split(",");
		}
		String[] mystring = new String[1];
		mystring[0] = string;
		return mystring;
	}
	
	public int getFreeSpace(Player player,ItemStack item) {
		Inventory inventory = player.getInventory();
		int free = 0;
		ItemStack[] contents = inventory.getContents();
		for (int i = 0;i<contents.length;i++) {
			ItemStack current = contents[i];
			if (current==null) {
				free+=item.getMaxStackSize();
			}
			else if (current.isSimilar(item)) {
				if (current.getAmount()<item.getMaxStackSize()) {
					free+=item.getMaxStackSize()-current.getAmount();
				}
			}
		}
		return free;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		Player player;
		if (sender instanceof Player==false) {
			player = null;
		}
		else {
			player = (Player) sender;
		}
		if (cmd.getName().equalsIgnoreCase("advshop")) {
			if (args.length>0) {
				if (args[0].equalsIgnoreCase("set" )) {
					if (args.length>3) {
						if (checkperm(player,"advshop.stock.set")==false) { 
							msg(player,getmsg("ERROR0").replace("{STRING}","advshop.stock.set"));
							return false;
						}
						Object[] result = LevensteinDistance(args[2]);
						String name = result[1]+"";
						ItemStack item;
						if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
							item = (ItemStack) result[0];
						}
						else {
							msg(player,getmsg("SEARCH1").replace("{STRING}",args[0].toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
							return true;
						}
						try {
							if (args[1].equalsIgnoreCase("sold")) {
								setSold(item, Integer.parseInt(args[3]));
								msg(player,getmsg("WARNING1"));
							}
							else if (args[1].equalsIgnoreCase("bought")) {
								setBought(item, Integer.parseInt(args[3]));
								msg(player,getmsg("WARNING1"));
							}
						}
						catch (Exception e) {
							msg(player,getmsg("ERROR3").replace("{INT}",args[3]+""));
						}
					}
					else {
						msg(player,getmsg("ERROR4").replace("{STRING}","&c/advshop set <bought|sold> <item> <value>"));
					}
				}
				else if (args[0].equalsIgnoreCase("reload" )) {
					if (checkperm(player,"advshop.reload")==false) { 
						msg(player,getmsg("ERROR0").replace("{STRING}","advshop.reload"));
						return false;
					}
					reloadConfig();
			        saveResource("english.yml", true);
			        File myfile = new File(getDataFolder() +File.separator+"idlist.yml");
			        if(myfile.exists()!=true) {  saveResource("idlist.yml", false); }
			        myfile = new File(getDataFolder() +File.separator+"history.yml");
			        if(myfile.exists()!=true) {  saveResource("history.yml", false); }
			        myfile = new File(getDataFolder() + File.separator+"prices.yml");
			        if(myfile.exists()!=true) {  saveResource("prices.yml", false); }
			        getConfig().options().copyDefaults(true);
			        final Map<String, Object> options = new HashMap<String, Object>();
			        getConfig().set("version", version);
			        options.put("item-similarity-match", 1);
			        options.put("default.bought", 128);
			        options.put("cross_map_trade", 128);
			        options.put("default.sold", 128);
			        options.put("fomula.buy", "((2*bought*totalvolume)/(volume*volume))");
			        options.put("fomula.sell", "((bought*totalvolume)/(volume*volume))");
			        options.put("language","english");
			        options.put("require-sold",true);
			        options.put("use-exp-instead",false);
			        options.put("require-stock-to-buy",false);
			        options.put("keep-history-days",30);
			        options.put("min-sell-price",0);
			        options.put("idlist","idlist");
			        for (final Entry<String, Object> node : options.entrySet()) {
			        	 if (!getConfig().contains(node.getKey())) {
			        		 getConfig().set(node.getKey(), node.getValue());
			        	 }
			        }
			        saveConfig();
			    	idlistFile = new File(getDataFolder()+File.separator+getConfig().getString("idlist")+".yml");
					idlist = YamlConfiguration.loadConfiguration(idlistFile);
					historyFile = new File(getDataFolder()+File.separator+"history.yml");
					pricesFile = new File(getDataFolder()+File.separator+"prices.yml");
					prices = YamlConfiguration.loadConfiguration(pricesFile);
					msg(player,getmsg("INFO0"));
					return true;
				}
			}
			msg(player,getmsg("INFO3"));
			return true;
		}
		if (cmd.getName().equalsIgnoreCase("hand")) {
			if (checkperm(player,"advshop.hand")==false) { 
				msg(player,getmsg("ERROR0").replace("{STRING}","&cadvshop.hand"));
				return false;
			}
			
			if (player == null) {
				msg(player,getmsg("ERROR5"));
				return false;
			}
			ItemStack item = player.getInventory().getItemInHand();
			String name;
			if (item==null) {
				name = "AIR";
			}
			else if (item.getTypeId()==0) {
				name = "AIR";
			}
			else if (item.getDurability()==0) {
				name = item.getType().name();
			}
			else {
				name = LevensteinDistance(item.getTypeId()+":"+item.getDurability())[1]+"";
			}
			item = new ItemStack(item.getTypeId(),1,item.getDurability());
			msg(player,getmsg("INFO4").replace("{STRING}",name).replace("{STRING2}",item.getTypeId()+"").replace("{STRING3}",item.getDurability()+""));
			return true;
		}
//		if (cmd.getName().equalsIgnoreCase("selltop")) {
//			Set<String> items = prices.getConfigurationSection("").getKeys(false);
//		}
//		if (cmd.getName().equalsIgnoreCase("buytop")) {
//			
//		}
		if (cmd.getName().equalsIgnoreCase("sellall")) {
			if (checkperm(player,"advshop.sellall")==false) { 
				msg(player,getmsg("ERROR0").replace("{STRING}","advshop.sellall"));
				return true;
			}
			if (player == null) {
				msg(player,getmsg("ERROR5"));
				return true;
			}
			Inventory inventory = player.getInventory();
			ItemStack[] contents = inventory.getContents();
			double money= 0;
			int total = 0;
			String timestamp = (Long.toString(System.currentTimeMillis()/1000L));
			String date = (new SimpleDateFormat("d\\M\\yy")).format(Calendar.getInstance().getTime());
			String time = (new SimpleDateFormat("H:m:s")).format(Calendar.getInstance().getTime());
			String user = player.getName();
			String buysell = "SELL";
			for (int i =0;i<contents.length;i++) {
				ItemStack item = contents[i];
				if (item!=null) {
					if (item.getType().equals(Material.AIR)==false) {
						int amount = item.getAmount();
						if (item.isSimilar(new ItemStack(item.getTypeId(), amount, item.getDurability()))) {
							total+=amount;
							for (int j = 0;j<amount;j++) {
								item.setAmount(1);
								money+=getSellValue(item);
								Sell(item, player);
								try {
									String itemid = item.getTypeId()+"";
									String name;
									if (item.getDurability()==0) {
										name =item.getType().name();
									}
									else {
										itemid+="-"+item.getDurability();
										name = LevensteinDistance(item.getTypeId()+":"+item.getDurability())[1]+"";
									}
									addLastLine(historyFile, timestamp+" - "+date+" - "+time+" - "+user+" - "+buysell+" - "+total+" - "+name+" - "+itemid+" - "+"$"+(money));
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							inventory.clear(i);
						}
					}
				}
			}
			if (total>0) {
				player.updateInventory();
				if (getConfig().getBoolean("use-exp-instead")) {
					ExperienceManager expMan = new ExperienceManager(player);
					expMan.changeExp(money);
					msg(player,getmsg("INFO5").replace("{INT}",""+total).replace("{STRING}",concatDouble(money+"")+" exp"));
				}
				else {
					econ.depositPlayer(player.getName(), money);
					msg(player,getmsg("INFO5").replace("{INT}",""+total).replace("{STRING}","$"+concatDouble(money+"")));
				}
				try {
					idlist.save(idlistFile);
					prices.save(pricesFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				msg(player,getmsg("ERROR6"));
			}
		}
		if (cmd.getName().equalsIgnoreCase("advsell")) {
			if (args.length>0) {
				if (checkperm(player,"advshop.sell")==false) { 
					msg(player,getmsg("ERROR0").replace("{STRING}","advshop.sell"));
					return true;
				}
				
				if (player == null) {
					msg(player,getmsg("ERROR5"));
					return true;
				}
				ItemStack item;
				String name = "";
				if (args[0].equalsIgnoreCase("hand")) {
					item = new ItemStack(player.getItemInHand().getType(), 1, player.getItemInHand().getDurability());
					if (item.getDurability()==0) {
						name =item.getType().name();
					}
					else {
						name = LevensteinDistance(item.getTypeId()+":"+item.getDurability())[1]+"";
					}
				}
				else {
					Object[] result = LevensteinDistance(args[0]);
					name = result[1]+"";
					if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
						item = (ItemStack) result[0];
					}
					else {
						msg(player,getmsg("SEARCH1").replace("{STRING}",args[0].toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
						return true;
					}
				}
				if (name.equals("null")) {
					msg(player,getmsg("ERROR7"));
					return true;
				}
				if (item==null) {
					msg(player,getmsg("ERROR4").replace("{STRING}","/advsell <hand|item> <amount>"));
					return true;
				}
				if (item.getType().equals(Material.AIR)==false) {
					Inventory inventory = player.getInventory();
					if (inventory.containsAtLeast(item, 1)) {
						int amount = 0;
						int total = 0;
						if (args.length>1) {
							try {
								amount = Integer.parseInt(args[1]);
							}
							catch (Exception e) {
								msg(player,getmsg("ERROR3").replace("{INT}",args[1]+""));
								return true;
							}
							for (ItemStack current:inventory.getContents()) {
								if (current!=null) {
									if (current.isSimilar(item)) {
										total+=current.getAmount();
									}
								}
							}
						}
						else {
							for (ItemStack current:inventory.getContents()) {
								if (current!=null) {
									if (current.isSimilar(item)) {
										amount+=current.getAmount();
									}
								}
							}
							total = amount;
						}
						if (total<amount) {
							msg(player,getmsg("ERROR8").replace("{STRING}",name).replace("{INT}",total+""));
							return true;
						}
						double money = 0;
						for (int i = 0;i<amount;i++) {
							item.setAmount(1);
							money+=getSellValue(item);
							Sell(item, player);
						}
						total = amount;
						for (int i = 0;i < inventory.getContents().length;i++) {
							ItemStack current = inventory.getContents()[i];
							if (current!=null) {
								if (current.isSimilar(item)) {
									if (amount>0) {
										if (current.getAmount()<amount) {
											amount-=current.getAmount();
											inventory.clear(i);
										}
										else {
											if (current.getAmount()==amount) {
												inventory.clear(i);
											}
											else {
												current.setAmount(current.getAmount()-amount);
											}
											amount = 0;
											break;
										}
									}
								}
							}
						}
						player.updateInventory();
						String ecosymbol = "$";
						if (getConfig().getBoolean("use-exp-instead")) {
							ecosymbol = "exp ";
							ExperienceManager expMan = new ExperienceManager(player);
							expMan.changeExp(money);
							msg(player,getmsg("INFO6").replace("{INT}",""+total).replace("{STRING}",name).replace("{STRING2}",concatDouble(money+"")+" exp"));
						}
						else {
							econ.depositPlayer(player.getName(), money);
							msg(player,getmsg("INFO6").replace("{INT}",""+total).replace("{STRING}",name).replace("{STRING2}","$"+concatDouble(money+"")));
						}
						
						try {
							idlist.save(idlistFile);
							prices.save(pricesFile);
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							String timestamp = (Long.toString(System.currentTimeMillis()/1000L));
							String date = (new SimpleDateFormat("d\\M\\yy")).format(Calendar.getInstance().getTime());
							String time = (new SimpleDateFormat("H:m:s")).format(Calendar.getInstance().getTime());
							String user = player.getName();
							String buysell = "SELL";
							String itemid = item.getTypeId()+"";
							if (item.getDurability()!=0) {
								itemid+="-"+item.getDurability();
							}
							addLastLine(historyFile, timestamp+" - "+date+" - "+time+" - "+user+" - "+buysell+" - "+total+" - "+name+" - "+itemid+" - "+ecosymbol+(money));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					else {
						msg(player,getmsg("ERROR9").replace("{STRING}",name));
						return true;
					}
				}
				else {
					msg(player,getmsg("ERROR10").replace("{STRING}","AIR"));
					return true;
				}
			}
			else {
				msg(player,getmsg("ERROR4").replace("{STRING}","/advsell <hand|item> <amount>"));
				return true;
			}
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("advbuy")) {
			if (args.length==2) {
				if (checkperm(player,"advshop.sell")==false) { 
					msg(player,getmsg("ERROR0").replace("{STRING}","advshop.buy"));
					return true;
				}
				if (player == null) {
					msg(player,getmsg("ERROR5"));
					return true;
				}
				Inventory inventory = player.getInventory();
				int amount = 0;
				int free = 0;
				String name;
				ItemStack item;
				try {
					amount = Integer.parseInt(args[1]);
				}
				catch (Exception e) {
					msg(player,getmsg("ERROR3").replace("{INT}",args[1]+""));
					return true;
				}
				Object[] result = LevensteinDistance(args[0]);
				name = result[1]+"";
				if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
					item = (ItemStack) result[0];
				}
				else {
					msg(player,getmsg("SEARCH1").replace("{STRING}",args[0].toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
					return true;
				}
				if (item==null) {
					msg(player,getmsg("ERROR4").replace("{STRING}","/advbuy <item> <amount>"));
					return true;
				}
				if (item.getType().equals(Material.AIR)==false) {
					double money = 0;
					boolean hasenough = true;
					int total = 0;
					boolean usingexp = false;
					ExperienceManager expMan = new ExperienceManager(player);
					if (getConfig().getBoolean("use-exp-instead")) {
						money = expMan.getCurrentExp();
						usingexp = true;
					}
					else {
						money = econ.getBalance(player.getName());
					}
					double totalmoney = money;
					free = getFreeSpace(player, item);
					if (free<amount) {
						if (free == 0) {
							msg(player,getmsg("ERROR11"));
						}
						else {
							msg(player,getmsg("ERROR12").replace("{INT}",""+free).replace("{STRING}",name));
						}
						return true;
					}
					if (getBuyValue(item)>money) {
						if (usingexp) {
							msg(player,getmsg("ERROR13"));
						}
						else {
							msg(player,getmsg("ERROR14"));
						}
						return true;
					}
					else {
						item.setAmount(1);
						for (int i = 0;i<amount;i++) {
							double price = getBuyValue(item);
							if (money >= price) {
								money-=price;
								boolean successful = Buy(item, player);
								if (successful==false) {
									money+=price;
									if (total> 0) {
										msg(player,getmsg("ERROR15").replace("{INT}",""+total).replace("{STRING}",name));
									}
									else {
										msg(player,getmsg("ERROR16").replace("{STRING}",name));
										return true;
									}
									break;
								}
								else {
									total++;
								}
							}
							else {
								hasenough = false;
								break;
							}
						}
						if (hasenough == false) {
							msg(player,getmsg("ERROR15").replace("{INT}",""+total).replace("{STRING}",name));
						}
						String ecosymbol = "$";
						if (usingexp) {
							expMan.setExp(money);
							ecosymbol = "exp ";
							msg(player,getmsg("INFO8").replace("{INT}",""+total).replace("{STRING}",name).replace("{STRING2}",concatDouble(money+"")+" exp"));
						}
						else {
							econ.withdrawPlayer(player.getName(), totalmoney-money);
							msg(player,getmsg("INFO8").replace("{INT}",""+total).replace("{STRING}",name).replace("{STRING2}","$"+concatDouble(money+"")));
						}
						item.setAmount(total);
						inventory.addItem(item);
						player.updateInventory();
						try {
							String timestamp = (Long.toString(System.currentTimeMillis()/1000L));
							String date = (new SimpleDateFormat("d\\M\\yy")).format(Calendar.getInstance().getTime());
							String time = (new SimpleDateFormat("H:m:s")).format(Calendar.getInstance().getTime());
							String user = player.getName();
							String buysell = "BUY";
							String itemid = item.getTypeId()+"";
							if (item.getDurability()!=0) {
								itemid+="-"+item.getDurability();
							}
							addLastLine(historyFile, timestamp+" - "+date+" - "+time+" - "+user+" - "+buysell+" - "+total+" - "+name+" - "+itemid+" - "+ecosymbol+(totalmoney-money));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						try {
							idlist.save(idlistFile);
							prices.save(pricesFile);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					msg(player,getmsg("ERROR17").replace("{STRING}","AIR"));
					return true;
				}
				
			}
			else {
				msg(player,getmsg("ERROR4").replace("{STRING}","/advbuy <item> <amount>"));
			}
		}
		else if (cmd.getName().equalsIgnoreCase("value")) {
			if (checkperm(player,"advshop.value")==false) { 
				msg(player,getmsg("ERROR0").replace("{STRING}","advshop.value"));
				return false;
			}
			
			if (player == null) {
				msg(player,getmsg("ERROR5"));
				return false;
			}
			if (args.length>0) {
				ItemStack item;
				if (args[0].equalsIgnoreCase("hand" )) {
					item = player.getItemInHand();
				}
				else {
					Object[] result = LevensteinDistance(args[0]);
					if ((Integer) result[2] <= getConfig().getInt("item-similarity-match")) {
						item = (ItemStack) result[0];
					}
					else {
						msg(player,getmsg("SEARCH1").replace("{STRING}",args[0].toLowerCase()).replace("{STRING2}",result[1].toString().toLowerCase().replace(" ", "_")));
						return false;
					}
				}
				String name;
				if (item==null) {
					name = "AIR";
				}
				else if (item.getTypeId()==0) {
					name = "AIR";
				}
				else if (item.getDurability()==0) {
					name = item.getType().name();
				}
				else {
					name = LevensteinDistance(item.getTypeId()+":"+item.getDurability())[1]+"";
				}
				item = new ItemStack(item.getTypeId(),1,item.getDurability());
				String itemid = item.getTypeId()+"";
				if (item.getDurability()!=0) {
					itemid+="-"+item.getDurability();
				}
				msg(player,getmsg("INFO4").replace("{STRING}",name).replace("{STRING2}",item.getTypeId()+"").replace("{STRING3}",item.getDurability()+""));
				try {
					msg(player,getmsg("INFO9").replace("{INT}",concatDouble(""+getSellValue(item))));
					msg(player,getmsg("INFO10").replace("{INT}",concatDouble(""+getBuyValue(item))));
					try {
						msg(player,getmsg("INFO11").replace("{INT}",prices.getString(itemid+".bought")));
						msg(player,getmsg("INFO12").replace("{INT}",prices.getString(itemid+".sold")));
					}
					catch (Exception e) {
						
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					msg(player,getmsg("ERROR18"));
					msg(player,getmsg("ERROR19"));
				}
				return true;
			}
			else {
				msg(player,getmsg("ERROR4").replace("{STRING}","&c/value <hand|item>"));
			}
		}
		else if(cmd.getName().equalsIgnoreCase("xpp")){
    		if (checkperm(player,"advshop.xpp")) {
    		if (args.length != 2){
    			msg(player,getmsg("ERROR4").replace("{STRING}","&c/xpp <player> <amount>"));
    		}
    		else {
    			if (player.getName().equalsIgnoreCase(args[0])!= true) {
    				List<Player> matches = getServer().matchPlayer(args[0]);
    				if (matches.isEmpty()) {
    					msg(player,getmsg("ERROR20").replace("{STRING}",args[0]));
    					msg(player,"&6No online player found for: &c"+args[0]);
    				}
    				else if (matches.size() > 1) {
    					msg(player,getmsg("ERROR21").replace("{STRING}",args[0]));
    				}
    				else {
    					Player user = matches.get(0);
    					if ((user.getWorld() == (player).getWorld()) || (this.getConfig().getString("cross_map_trade").equalsIgnoreCase("true"))) {
          				  try {
    						if ((player).getTotalExperience() >= Integer.parseInt(args[1])) {
    							if (Integer.parseInt(args[1])>0) {
    							      int myxp = (player).getTotalExperience();
    							 
    							      (player).setTotalExperience(0);
    							      (player).setLevel(0);
    							      (player).setExp(0);
    							      (player).giveExp(myxp - Integer.parseInt(args[1]));
    							      msg(player,getmsg("INFO13").replace("{INT}",""+(myxp-Integer.parseInt(args[1]))));
    							      msg(player,getmsg("INFO14").replace("{INT}",""+(Integer.parseInt(args[1]))));
    							      user.giveExp(Integer.parseInt(args[1]));
    							}
    							else {
    								msg(player,getmsg("ERROR24").replace("{INT}",""+args[1]));
    								msg(player,"&6Amount must be positive: &c"+args[1]+".");
    							}
          				  }
    						else {
    							msg(player,getmsg("ERROR25").replace("{INT}",""+args[1]));
    						}
          				  }
          				  catch (Exception e) {
          					msg(player,getmsg("ERROR3").replace("{INT}",args[1]+""));
          				  }
    					}
    					else {
    						msg(player,getmsg("ERROR22"));
    					}
    				}
    			}
    			else {
    				msg(player,getmsg("ERROR23"));
    			}
    		}
    		}
    		else {
    			msg(player,getmsg("ERROR0").replace("{STRING}","advshop.xpp"));
    		}
    	}
		
		
		
		return false;
	}
	
	
	
	
}