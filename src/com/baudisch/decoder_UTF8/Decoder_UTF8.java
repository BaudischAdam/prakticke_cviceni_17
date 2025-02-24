package com.baudisch.decoder_UTF8;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Formatter;

public class Decoder_UTF8 {
    /// zadané parametry
    private static final String OUTPUT_DIRECTORY = System.getProperty("user.home") + "/Java_Outputs";
    private static final String OUTPUT_FILE = OUTPUT_DIRECTORY + "/dopis_utf8.txt";
    private static final String INPUT_FILE = "com/baudisch/inputs/dopis.bin";
    private static final String EXPECTED_HASH = "FADA5D5E2C76DED0D873D1AD987339A4A9DE57C77EC0AB740A386CFB866507BA";
    private static final String[] ENCODINGS = {"IBM861", "Cp869", "Cp500", "IBM870", "IBM284"};

    public static void main(String[] args) {

        /// Hledání souboru - input stream zvolen protože dopis.bin je v JARU
        try (InputStream inputStream = Decoder_UTF8.class.getClassLoader().getResourceAsStream(INPUT_FILE)) {
            if (inputStream == null) {
                System.err.println("Chyba! nebyl nalezen soubor: " + INPUT_FILE);
                return;
            }

            System.out.println("Soubor nalezen: " + INPUT_FILE);
            byte[] data = inputStream.readAllBytes();

            /// kontrola SHA
            if (!kontrolaSHA256(data)) {
                System.err.println("Chyba: Nesouhlasí SHA kontrola");
                return;
            }

            String decodedText = dekodujSoubor(data);
            if (decodedText != null) {
                ulozDoSlozky(decodedText);
                System.out.println("Dekódování úspěšné = Výstup uložen " + OUTPUT_FILE);
            } else {
                System.err.println("Nepodařilo se dekódovat soubor.");
            }
        } catch (IOException e) {
            System.err.println("Chyba při čtení složky: " + e.getMessage());
        }
    }


    /// Metoda pro ovvěřování SHA - Tady jsem byl celkem mimo :D
    private static boolean kontrolaSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return byteDoHex(hash).equalsIgnoreCase(EXPECTED_HASH);
        } catch (Exception e) {
            System.err.println("Chyba při kontrole SHA-256: " + e.getMessage());
            return false;
        }
    }

    /// Metoda pro zpracování bytů - tady jsem byl taky celkem ztracený
    private static String byteDoHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString().toUpperCase();
        formatter.close();
        return result;
    }

    /// Metoda pro dekdoování - zvolen způsob score systému
    private static String dekodujSoubor(byte[] data) {
        String bestText = null;
        /// iniciace špatného hodnocení
        int bestScore = -1;

        for (String encoding : ENCODINGS) {
            try {
                String text = new String(data, Charset.forName(encoding));
                int score = controlTextQuality(text);

                System.out.println("Kódování: " + encoding + " (skóre: " + score + ")");

                /// zapsiování nejlepšího score
                if (score > bestScore) {
                    bestScore = score;
                    bestText = text;
                }
            } catch (Exception e) {
                System.err.println("Chyba při dekódování: " + encoding + " = " + e.getMessage());
            }
        }
        return bestText;
    }

    /// Kontrola zda se v textu objevují české znaky (přepdoklad českého dopisu)
    private static int controlTextQuality(String text) {
        ///
        int diaPocet = (int) text.chars().filter(c -> "ěščřžýáíéůúďťňó".indexOf(c) >= 0).count();
        int pocetNechtenychZnaku = (int) text.chars().filter(c -> c == '?' || c == 'ÿ' || c == '½' || c == '¶').count();
        return diaPocet - pocetNechtenychZnaku;
    }

    /// Ukladani souboru dekodovaneho
    private static void ulozDoSlozky(String text) {
        /// Vytváření složky
        try {
            Path outputDir = Paths.get(OUTPUT_DIRECTORY);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                System.out.println("Vytvořena složka: " + OUTPUT_DIRECTORY);
            }

            /// ukladani souvoru
            System.out.println("Ukládám soubor do: " + OUTPUT_FILE);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, Charset.forName("UTF-8")))) {
                writer.write(text);
            }
        } catch (IOException e) {
            System.err.println("Chyba při ukládání souboru: " + e.getMessage());
        }
    }
}
