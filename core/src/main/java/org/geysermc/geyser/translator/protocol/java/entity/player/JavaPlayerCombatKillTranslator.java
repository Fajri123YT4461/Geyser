/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.java.entity.player;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.nukkitx.protocol.bedrock.packet.DeathInfoPacket;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

@Translator(packet = ClientboundPlayerCombatKillPacket.class)
public class JavaPlayerCombatKillTranslator extends PacketTranslator<ClientboundPlayerCombatKillPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundPlayerCombatKillPacket packet) {
        if (packet.getPlayerId() == session.getPlayerEntity().getEntityId() && GameProtocol.supports1_19_10(session)) {
            Component deathMessage = packet.getMessage();
            // TODO - could inject score in, but as of 1.19.10 newlines don't center and start at the left of the first text
            DeathInfoPacket deathInfoPacket = new DeathInfoPacket();
            deathInfoPacket.setCauseAttackName(MessageTranslator.convertMessage(deathMessage, session.locale()));
            session.sendUpstreamPacket(deathInfoPacket);
        }
    }
}
