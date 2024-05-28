import java.io.*;
import java.util.*;


public class PasswordCrack {

    public static void main(String[] args) {
        try {//Initialize dictionary and password files
            String dictFile = args[0];
            String passwdFile = args[1];

            Set<String> dictWords = readFile(dictFile);  // Read dictionary words into a List
            Set<String> passwdLines = readFile(passwdFile);    // Read password file lines into a List
            Set<User> users = parsePasswds(passwdLines);        // Parse the password lines into User objects

            crackPasswd(dictWords, users);

        } catch (IOException e) {
            System.out.println("File read error: " + e.getMessage());
        } catch (
                IndexOutOfBoundsException e) {
            System.out.println("Please provide dictionary and password file: java PasswordCrack <dictFile> <passwdFile>");
        }
    }

    //Lets us read the lines in the files and store in a Set
    private static Set<String> readFile(String filePath) throws IOException {
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
        }
        return words;
    }

    private static Set<String> getCommonPasswords() {
        Set<String> commonPasswords = new HashSet<>();
        commonPasswords.add("password");
        commonPasswords.add("123456");
        commonPasswords.add("qwerty");
        commonPasswords.add("abc123");
        commonPasswords.add("letmein");
        commonPasswords.add("111111");
        commonPasswords.add("baseball");
        commonPasswords.add("iloveyou");
        commonPasswords.add("admin");
        commonPasswords.add("123454321");
        commonPasswords.add("abcdefg");

        /*
        Commenting out as I just had these here for testing purposes
        //commonPasswords.add("zrone"); //took an hour to find, so adding manually
        //commonPasswords.add("uNONEVENT"); //took an hour to find so adding manually
        //commonPasswords.add("elurreerrule"); //Took 2-3 hours to find so adding manually
         */
        return commonPasswords;
    }

    //Extract username, salt, and password hash
    private static Set<User> parsePasswds(Set<String> passwds) {
        Set<User> users = new HashSet<>();
        for (String entry : passwds) {
            String[] components = entry.split(":");
            String username = components[0];
            String passwdHash = components[1];
            //Extract 2 bit salt from data
            String salt = passwdHash.substring(0, 2);
            users.add(new User(username, salt, passwdHash));
        }
        return users;
    }

    //Cracking logic including mangler
    private static void crackPasswd(Set<String> dictWords, Set<User> users) {
        Set<String> crackedPairs = new HashSet<>(); //User/passwd pairs we have cracked
        Set<String> common = getCommonPasswords();
        Set<String> allWords = new HashSet<>(dictWords);
        allWords.addAll(common);

        //Iterate over each word in dict
        for (String word : allWords) {
            for (User user : users) {
                if (!crackedPairs.contains(user.username)) { //Check if we have already cracked a user
                    if (tryCrack(user, word)) {
                        //System.out.println("Cracked user: " + user.username + ", Password: " + word);//Prints cracked user and password
                        System.out.println(word);
                        crackedPairs.add(user.username); //Mark user as cracked
                    }
                }
            }
        }
        //Mangler
        for (int mangleLvl = 1; mangleLvl <= 3; mangleLvl++) {
            //System.out.println("Starting mangle level " + mangleLvl);
            //Similar logic to previous function, but now we compare to the mangled word rather than dictionary
            for (String word : allWords) {
                Set<String> mangledWords = mangleWord(word, mangleLvl);
                for (String mangledWord : mangledWords) {
                    for (User user : users) {
                        if (!crackedPairs.contains(user.username)) {
                            if (tryCrack(user, mangledWord)) {
                                //System.out.println("Cracked user: " + user.username + ", Password: " + mangledWord); //Prints cracked user and (mangled) password
                                System.out.println(mangledWord);
                                crackedPairs.add(user.username); //Mark user as cracked
                            }
                        }
                    }
                }
            } //System.out.println("Done with mangle level " + mangleLvl);
        }
    }

    //Mangler logic
    private static Set<String> mangleWord(String word, int mangleLvl) {
        Set<String> mangledWords = new HashSet<>();
        mangledWords.add(word);

        if (mangleLvl == 0) {
            return mangledWords;
        }

        for (int i = 0; i < mangleLvl; i++) {
            Set<String> tempMangledWords = new HashSet<>(mangledWords.size() * 2);
            for (String mangledWord : mangledWords) {
                tempMangledWords.addAll(applyMangleOperations(mangledWord));
            }
            mangledWords = tempMangledWords;
        }

        return mangledWords;
    }

    private static Set<String> applyMangleOperations(String word) {
        Set<String> mangledWords = new HashSet<>();


        //Append 123 as this is a common thing to do
        mangledWords.add(word + "123");
        mangledWords.add("123" + word);

        // Prepend/Append characters as numbers
        for (char c = '0'; c <= '9'; c++) {
            mangledWords.add(new StringBuilder(word).insert(0, c).toString());
            mangledWords.add(word + c);
        }
        // Prepend/Append characters lowercase letters
        for (char c = 'a'; c <= 'z'; c++) {
            mangledWords.add(new StringBuilder(word).insert(0, c).toString());
            mangledWords.add(word + c);
            mangledWords.add(new StringBuilder(word).insert(0, Character.toUpperCase(c)).toString());
            mangledWords.add(word + Character.toUpperCase(c));
        }
        // Prepend/append characters uppercase letters
        for (char c = 'A'; c <= 'Z'; c++) {
            mangledWords.add(new StringBuilder(word).insert(0, c).toString());
            mangledWords.add(word + c);
        }

        // Delete first/last character
        if (word.length() > 1) {
            mangledWords.add(word.substring(1));
            mangledWords.add(word.substring(0, word.length() - 1));
        }

        // Reverse, duplicate, and reflect
        String reversed = new StringBuilder(word).reverse().toString();
        mangledWords.add(reversed);
        mangledWords.add(word + word);
        mangledWords.add(word + reversed);
        mangledWords.add(reversed + word);

        // Upper/Lower case, capitalize, and toggle case
        mangledWords.add(word.toUpperCase());
        mangledWords.add(word.toLowerCase());
        mangledWords.add(capitalize(word));
        mangledWords.add(nCap(word));
        mangledWords.add(caseToggle(word, true));
        mangledWords.add(caseToggle(word, false));

        return mangledWords;
    }

    //Helpers
    private static String capitalize(String word) {
        if (word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    private static String nCap(String word) {
        if (word.isEmpty()) return word;
        return Character.toLowerCase(word.charAt(0)) + word.substring(1).toUpperCase();
    }

    private static String caseToggle(String word, boolean startLower) {
        StringBuilder tgl = new StringBuilder();
        boolean lower = startLower;
        for (char c : word.toCharArray()) {
            tgl.append(lower ? Character.toLowerCase(c) : Character.toUpperCase(c));
            lower = !lower;
        }
        return tgl.toString();
    }

    //Cracking logic
    private static boolean tryCrack(User user, String guess) {
        String truncatedGuess = guess.length() > 8 ? guess.substring(0, 8) : guess;
        String hashedPasswd = jcrypt.crypt(user.salt, truncatedGuess);
        return hashedPasswd.equals(user.passwdHash);
    }

    //Class User stores information about each user needed for cracking passwords
    static class User {
        String username;
        String salt;
        String passwdHash;

        User(String username, String salt, String passwdHash) {
            this.username = username;
            this.salt = salt;
            this.passwdHash = passwdHash;
        }

    }
}
