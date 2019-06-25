# VirtualChestEnchantments

[![license](https://img.shields.io/github/license/ustc-zzzz/VirtualChestEnchantments.svg)](https://github.com/ustc-zzzz/VirtualChestEnchantments/blob/master/LICENSE) [![ore](https://img.shields.io/badge/dynamic/json.svg?color=blue&label=ore&prefix=v&query=%24%5B0%5D.name&url=https%3A%2F%2Fore.spongepowered.org%2Fapi%2Fv1%2Fprojects%2Fvcench%2Fversions)](https://ore.spongepowered.org/zzzz/VirtualChestEnchantments/versions) [![discord](https://img.shields.io/discord/570993846524182530.svg?color=purple)](https://discord.gg/TftabgG)

An addon for VirtualChest that adds a way for enchanting items while an action is submitted

The version of VirtualChest that the plugin depend on should be no lower than `1.0.0-rc-4`

Here is an example configuration file that utilizes VirtualChestEnchantments:

```hocon
virtualchest {
  Rows = 1
  TextTitle = "&fEfficiency V"
  UpdateIntervalTick = 20
  Slot4 {
    Item {
      ItemType = "minecraft:diamond_pickaxe", UnsafeDamage = 0, Count = 1
      DisplayName = "&aLeft of right click for enchanting the diamond pickaxe with Efficiency V"
    }
    Action {
      CommandBefore = "tell: &aEnchant Efficiency V for %player_name%"
      Command = "enchant: minecraft:efficiency:5"
      KeepInventoryOpen = true
      HandheldItem {
        ItemType = "minecraft:diamond_pickaxe"
        SearchInventory = true
      }
    }
  }
}
```
