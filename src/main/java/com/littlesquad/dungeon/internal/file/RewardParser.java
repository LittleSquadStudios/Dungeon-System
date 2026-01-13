package com.littlesquad.dungeon.internal.file;

import com.littlesquad.dungeon.api.rewards.AbstractReward;
import com.littlesquad.dungeon.api.rewards.ItemReward;
import com.littlesquad.dungeon.api.rewards.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("ClassCanBeRecord")
public final class RewardParser {

    private final FileConfiguration dungeonConf;
    private static final Logger LOGGER = Logger.getLogger(RewardParser.class.getName());
    private static final String DEBUG_PREFIX = "[RewardParser] ";

    RewardParser(final FileConfiguration dungeonConf) {
        this.dungeonConf = dungeonConf;
        LOGGER.info(DEBUG_PREFIX + "Inizializzato RewardParser");
    }

    /**
     * This method is used to parse a list of rewards starting from a dungeon config
     * he takes first the items then build them up and cache it all
     *
     * @since 1.0.0
     * @author LittleSquad
     */
    public List<Reward> parse() {
        LOGGER.info(DEBUG_PREFIX + "=== INIZIO PARSING REWARDS ===");
        try {throw new RuntimeException();} catch (final RuntimeException e) {e.printStackTrace();}

        // Initializing array of rewards
        Reward[] rewards = new Reward[0];

        // Taking from parser the dungeon section about rewards
        final ConfigurationSection section = dungeonConf.getConfigurationSection("rewards");

        // Checking if configuration has this section
        if (section == null) {
            LOGGER.warning(DEBUG_PREFIX + "ATTENZIONE: Sezione 'rewards' non trovata nella configurazione!");
            LOGGER.warning(DEBUG_PREFIX + "Chiavi disponibili nel root: " + dungeonConf.getKeys(false));
            return Arrays.stream(rewards).toList();
        }

        LOGGER.info(DEBUG_PREFIX + "Sezione 'rewards' trovata correttamente");

        // Taking subsections of rewards so each reward
        final Set<String> subSections = section.getKeys(false);
        LOGGER.info(DEBUG_PREFIX + "Numero di rewards trovati: " + subSections.size());
        LOGGER.info(DEBUG_PREFIX + "Lista rewards: " + subSections);

        // Check if there's some rewards registered
        if (subSections.isEmpty()) {
            LOGGER.warning(DEBUG_PREFIX + "Nessun reward configurato nella sezione 'rewards'");
            return Arrays.stream(rewards).toList();
        }

        // Initializing rewards to reward final size taken from amount of rewards present in the config
        rewards = new Reward[subSections.size()];

        int i = 0;
        // Iterating along all subsections
        for (final String s : subSections) {
            LOGGER.info(DEBUG_PREFIX + "");
            LOGGER.info(DEBUG_PREFIX + ">>> PARSING REWARD #" + (i + 1) + ": '" + s + "' <<<");

            final String sub = "rewards." + s;
            LOGGER.info(DEBUG_PREFIX + "ID completo reward: " + sub);

            // Verifica esistenza sottosezione
            final ConfigurationSection rewardSection = section.getConfigurationSection(s);
            if (rewardSection == null) {
                LOGGER.severe(DEBUG_PREFIX + "ERRORE: Sottosezione '" + s + "' è null! Saltando reward...");
                continue;
            }

            // Log delle chiavi nella sezione reward
            LOGGER.info(DEBUG_PREFIX + "Chiavi nel reward '" + s + "': " + rewardSection.getKeys(false));

            // Initializing items rewards array
            ItemReward[] itemsR = new ItemReward[0];
            final ConfigurationSection itemSec = section.getConfigurationSection(s + ".items");

            // Item parsing
            LOGGER.info(DEBUG_PREFIX + "Controllo presenza items...");

            // Checking if there's some items in this reward
            if (itemSec == null) {
                LOGGER.warning(DEBUG_PREFIX + "Nessuna sezione 'items' trovata per il reward '" + s + "'");
            } else {
                final Set<String> items = itemSec.getKeys(false);
                LOGGER.info(DEBUG_PREFIX + "Trovati " + items.size() + " items nel reward '" + s + "'");
                LOGGER.info(DEBUG_PREFIX + "Lista items: " + items);

                if (!items.isEmpty()) {
                    itemsR = new ItemReward[items.size()];

                    int ia = 0;
                    for (final String item : items) {
                        LOGGER.info(DEBUG_PREFIX + "  -> BUILDING ITEM #" + (ia + 1) + ": '" + item + "'");

                        // Log di tutti i campi dell'item
                        String itemPath = item + ".";
                        LOGGER.info(DEBUG_PREFIX + "     is_mythic_item: " + itemSec.getBoolean(itemPath + "is_mythic_item"));
                        LOGGER.info(DEBUG_PREFIX + "     mythic_item_name: " + itemSec.getString(itemPath + "mythic_item_name"));
                        LOGGER.info(DEBUG_PREFIX + "     type: " + itemSec.getString(itemPath + "type"));
                        LOGGER.info(DEBUG_PREFIX + "     amount: " + itemSec.getInt(itemPath + "amount"));
                        LOGGER.info(DEBUG_PREFIX + "     is_glowing: " + itemSec.getBoolean(itemPath + "is_glowing"));
                        LOGGER.info(DEBUG_PREFIX + "     enchants: " + itemSec.getStringList(itemPath + "enchants"));
                        LOGGER.info(DEBUG_PREFIX + "     display_name: " + itemSec.getString(itemPath + "display_name"));
                        LOGGER.info(DEBUG_PREFIX + "     lore: " + itemSec.getStringList(itemPath + "lore"));

                        // Validazione campi critici
                        if (itemSec.getInt(itemPath + "amount") <= 0) {
                            LOGGER.warning(DEBUG_PREFIX + "     ATTENZIONE: amount è <= 0 per l'item '" + item + "'");
                        }

                        String type = itemSec.getString(itemPath + "type");
                        if (type == null || type.isEmpty()) {
                            LOGGER.warning(DEBUG_PREFIX + "     ATTENZIONE: type è null o vuoto per l'item '" + item + "'");
                        }

                        boolean isMythic = itemSec.getBoolean(itemPath + "is_mythic_item");
                        String mythicName = itemSec.getString(itemPath + "mythic_item_name");
                        if (isMythic && (mythicName == null || mythicName.isEmpty())) {
                            LOGGER.warning(DEBUG_PREFIX + "     ATTENZIONE: is_mythic_item è true ma mythic_item_name è null/vuoto");
                        }

                        // Building up the items
                        itemsR[ia++] = new ItemReward() {
                            @Override
                            public boolean isMythicItem() {
                                return itemSec.getBoolean(item + ".is_mythic_item");
                            }

                            @Override
                            public Optional<String> mythicItemName() {
                                return Optional.ofNullable(itemSec.getString(item + ".mythic_item_name"));
                            }

                            @Override
                            public Optional<String> type() {
                                return Optional.ofNullable(itemSec.getString(item + ".type"));
                            }

                            @Override
                            public int amount() {
                                return itemSec.getInt(item + ".amount");
                            }

                            @Override
                            public boolean isGlowing() {
                                return itemSec.getBoolean(item + ".is_glowing");
                            }

                            @Override
                            public List<String> enchantments() {
                                return itemSec.getStringList(item + ".enchants");
                            }

                            @Override
                            public String displayName() {
                                return itemSec.getString(item + ".display_name");
                            }

                            @Override
                            public List<String> lore() {
                                return itemSec.getStringList(item + ".lore");
                            }
                        };

                        LOGGER.info(DEBUG_PREFIX + "  -> Item '" + item + "' creato con successo");
                    }
                } else {
                    LOGGER.warning(DEBUG_PREFIX + "La sezione items esiste ma è vuota per il reward '" + s + "'");
                }
            }

            // Putting items into a copy array
            ItemReward[] finalItemsR = itemsR;
            LOGGER.info(DEBUG_PREFIX + "Totale items creati per '" + s + "': " + finalItemsR.length);

            // Log dei dati del reward
            double exp = section.getDouble(s + ".experience");
            List<String> cmds = section.getStringList(s + ".commands");
            LOGGER.info(DEBUG_PREFIX + "Experience per reward '" + s + "': " + exp);
            LOGGER.info(DEBUG_PREFIX + "Commands per reward '" + s + "': " + cmds);

            // Validazione
            if (exp < 0) {
                LOGGER.warning(DEBUG_PREFIX + "ATTENZIONE: experience è negativa per il reward '" + s + "'");
            }

            if (cmds.isEmpty()) {
                LOGGER.info(DEBUG_PREFIX + "Info: Nessun comando configurato per il reward '" + s + "'");
            }

            // Building the reward with the abstract class
            AbstractReward reward = new AbstractReward() {

                private final String id = sub;
                private final ItemReward[] itemsRewards = finalItemsR;
                private final double experience = section.getDouble(s + ".experience");
                private final List<String> commands = section.getStringList(s + ".commands");

                @Override
                public String id() {
                    return id;
                }

                @Override
                public List<ItemReward> rewards() {
                    return Arrays.stream(itemsRewards).toList();
                }

                @Override
                public double experience() {
                    return experience;
                }

                @Override
                public List<String> commands() {
                    return commands;
                }
            };

            rewards[i] = reward;
            LOGGER.info(DEBUG_PREFIX + "Reward '" + s + "' aggiunto all'array alla posizione " + i);

            i++;
        }

        List<Reward> finalList = Arrays.stream(rewards).toList();
        LOGGER.info(DEBUG_PREFIX + "");
        LOGGER.info(DEBUG_PREFIX + "=== FINE PARSING REWARDS ===");
        LOGGER.info(DEBUG_PREFIX + "Totale rewards parsati con successo: " + finalList.size());

        return finalList;
    }
}