# TFTH Performance Fix <span style="color:#e03e2d">(Unofficial Patch)</span>

A companion mod for **The Flesh That Hates** 1.1b (Forge 1.20.1) that makes it <span style="color:#2dc26b"><strong>run faster</strong></span> and <span style="color:#2dc26b"><strong>fixes some of its bugs</strong></span>. <span style="color:#e03e2d"><strong>The Flesh That Hates itself is still required.</strong></span>

***

## Requirements

<table>
  <tr>
    <th align="left">Requirement</th>
    <th align="left">Version</th>
  </tr>
  <tr>
    <td><strong>Minecraft</strong></td>
    <td>1.20.1 (Forge)</td>
  </tr>
  <tr>
    <td><strong>The Flesh That Hates</strong></td>
    <td><a href="https://www.curseforge.com/minecraft/mc-mods/the-flesh-that-hates">1.1b</a></td>
  </tr>
</table>

***

## Before And After

Same spot and settings, without the patch and then with it. The FPS counter is in the top left.

### A field of about 2,500 flesh spikes: <span style="color:#e03e2d">16 FPS</span> to <span style="color:#2dc26b">43 FPS</span> average

<table>
  <tr>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s2_before.jpg" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s2_before.jpg" alt="Spike field without the patch" width="400"></a></td>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s2_after.jpg" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s2_after.jpg" alt="Spike field with the patch" width="400"></a></td>
  </tr>
  <tr>
    <td align="center"><strong><span style="color:#e03e2d">Without</span></strong>: <strong>16 FPS</strong> average</td>
    <td align="center"><strong><span style="color:#2dc26b">With</span></strong>: <strong>43 FPS</strong> average</td>
  </tr>
</table>

### A pen full of flesh mobs: <span style="color:#e03e2d">153 FPS</span> to <span style="color:#2dc26b">164 FPS</span> average

<table>
  <tr>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s1_before.jpg" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s1_before.jpg" alt="Flesh mobs without the patch" width="400"></a></td>
    <td align="center"><a href="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s1_after.jpg" rel="nofollow"><img src="https://raw.githubusercontent.com/RinkyDinkyNooble/tfth-performance-fix/main/curseforge/images/s1_after.jpg" alt="Flesh mobs with the patch" width="400"></a></td>
  </tr>
  <tr>
    <td align="center"><strong><span style="color:#e03e2d">Without</span></strong>: <strong>153 FPS</strong> average</td>
    <td align="center"><strong><span style="color:#2dc26b">With</span></strong>: <strong>164 FPS</strong> average</td>
  </tr>
</table>

***

## Faster

*   **Flesh fog and flesh music** checks no longer read every block within 30 blocks of you. They skip the parts of the world with no flesh in them.
*   **Incubators** no longer read a whole box up to 251 blocks across for each mob they spawn, or go through every entity in the world to count their mobs.
*   **An incubator calling for help** no longer reads every block within 30 blocks to put out fire and lava.
*   **Infection spreads** do less work per block and keep their visited positions in about <span style="color:#2dc26b"><strong>60 times less memory</strong></span>.
*   **Flesh spikes that aren't moving** are drawn from a recording instead of being rebuilt every frame.
*   **Flesh mobs, spikes, boils and the flesh forge** are drawn without sorting their faces every frame wherever their texture allows it, and look the same. With shader packs that light see-through mobs differently (Complementary does), those are now lit like other mobs. Faces with no area are no longer drawn.

## Bug Fixes

*   **Infection spreads survive restarts.** The Flesh That Hates saved them in a form it couldn't read back, so every spread was lost at each restart.
*   **Spreads stay in their own dimension** after a restart instead of moving to the Overworld.
*   **A spread pauses** while its chunk isn't loaded or no incubator is near it, waits at chunks that aren't loaded instead of loading them, and is removed once its origin block is gone.
*   **Incubators only spawn mobs, and new incubators only appear, in loaded chunks.** Before, they could load or generate chunks in the middle of a tick, which caused <span style="color:#e03e2d"><strong>lag spikes</strong></span>.
*   **A starting incubator that evolves** no longer keeps its chunk loaded forever.
*   **No more "Modifier is already applied on this attribute!" crash** from the GOH effect.
*   **Brutes share damage** after the flesh stage damage reduction, not before it.
*   **Other players near flesh** no longer set your flesh fog or pause your music.
*   **The Flesh Unseen** is registered at its real size instead of 0 by 0. With Presence Footsteps, each Flesh Unseen searched an 81 by 81 block area under its feet for every step, which made the game <span style="color:#e03e2d"><strong>stutter</strong></span>.

The log has one line for each patched part of The Flesh That Hates, saying how many changes applied.

***

## Credit

This is an **unofficial patch**, not affiliated with The Flesh That Hates. [The Flesh That Hates](https://www.curseforge.com/minecraft/mc-mods/the-flesh-that-hates) is made by WinVic.
