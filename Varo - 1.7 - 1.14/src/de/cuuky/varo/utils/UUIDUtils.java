package de.cuuky.varo.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

public class UUIDUtils {

	public static UUID getUUID(String name) throws Exception {
		return getUUIDTime(name, -1);
	}
	
	public static String getNamesChanged(String name) throws Exception {
		ArrayList<UUID> UUIDs = getOldUUIDs(name);
		
		if (UUIDs == null || UUIDs.size() == 0) {
			return null;
		}
		
		//TODO FÜR JEDE UUID den aktuellen Namen herausfinden und zurückgeben! Außerdem wird ein String und das Datum der Umbenennung zurückgegeben
		
		return null;
	}
	
	private static UUID getUUIDTime(String name, long time) throws Exception {
		Scanner scanner;
		if (time == -1) {
			scanner = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream());
		} else {
			scanner = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + name + "?at=" + String.valueOf(time)).openStream());
		}
		
		if(scanner.hasNext()) {
			String s1 = scanner.nextLine();
			String s = s1.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "").split(",")[0].split(":")[1];
			scanner.close();
			return UUID.fromString(s.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5"));
		}
		
		scanner.close();
		return null;
	}
	
	private static ArrayList<UUID> getOldUUIDs(String name) throws Exception {
		Date Date = new Date();
		long Time = Date.getTime() / 1000;
		
		ArrayList UUIDs = new ArrayList<UUID>();
		
		for (int i = 0; i < 60*60*24*28; i++) {
			UUID UUID = getUUIDTime(name, (Time-i));
			if (UUID != null) {
				if (!UUIDs.contains(UUID)) {
					UUIDs.add(UUID);
				}
			}
		}
		
		return UUIDs;
	}
	
}