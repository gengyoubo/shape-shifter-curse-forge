"""Run actual animation service/client code against small transport/world fakes.

Requires Python 3 and JDK 17+. No Minecraft launch or downloaded test libraries.
The fakes model only external APIs; playback/expiry/replication logic is production code.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "net.onixary.shapeShifterCurseForge"
BASE = PACKAGE.replace(".", "/")
STUBS = {
    "net/minecraft/resources/ResourceLocation.java": '''
package net.minecraft.resources;
public record ResourceLocation(String value) {
 public static ResourceLocation fromNamespaceAndPath(String ns, String path) { return new ResourceLocation(ns+":"+path); }
 public static ResourceLocation tryParse(String value) { return value.isEmpty()?null:new ResourceLocation(value); }
 public String toString() { return value; }
}''',
    "net/minecraft/client/multiplayer/ClientLevel.java": '''
package net.minecraft.client.multiplayer;
public class ClientLevel { public long now; public long getGameTime() { return now; } }
''',
    "net/minecraft/world/entity/player/Player.java": '''
package net.minecraft.world.entity.player;
import net.minecraft.client.multiplayer.ClientLevel;
public class Player {
 public final int id; public final ClientLevel level;
 public Player(int id, ClientLevel level) { this.id=id; this.level=level; }
 public int getId(){return id;} public ClientLevel level(){return level;}
}''',
    "net/minecraft/server/level/ServerPlayer.java": '''
package net.minecraft.server.level;
public class ServerPlayer {
 private final java.util.UUID id = java.util.UUID.randomUUID();
 public int tickCount; public java.util.UUID getUUID(){return id;}
}''',
    "net/minecraft/client/Minecraft.java": '''
package net.minecraft.client;
import net.minecraft.client.multiplayer.ClientLevel;
public class Minecraft {
 private static final Minecraft INSTANCE = new Minecraft();
 public ClientLevel level; public static Minecraft getInstance(){return INSTANCE;}
}''',
    "net/minecraft/network/FriendlyByteBuf.java": '''
package net.minecraft.network;
public class FriendlyByteBuf {
 private final java.util.ArrayList<Object> values = new java.util.ArrayList<>(); private int index;
 public void writeInt(int x){values.add(x);} public int readInt(){return (int)values.get(index++);}
 public void writeVarInt(int x){writeInt(x);} public int readVarInt(){return readInt();}
 public void writeUtf(String x,int limit){values.add(x);} public String readUtf(int limit){return (String)values.get(index++);}
 public void writeEnum(Enum<?> x){values.add(x);} public <T extends Enum<T>> T readEnum(Class<T> cls){return cls.cast(values.get(index++));}
 public void writeBoolean(boolean x){values.add(x);} public boolean readBoolean(){return (boolean)values.get(index++);}
}''',
    "net/minecraftforge/api/distmarker/Dist.java": 'package net.minecraftforge.api.distmarker; public enum Dist { CLIENT }',
    "net/minecraftforge/fml/DistExecutor.java": '''
package net.minecraftforge.fml;
public class DistExecutor {
 public static void unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist d, java.util.function.Supplier<Runnable> r){r.get().run();}
}''',
    "net/minecraftforge/network/NetworkEvent.java": '''
package net.minecraftforge.network;
public class NetworkEvent {
 public static class Context { public void enqueueWork(Runnable r){r.run();} public void setPacketHandled(boolean b){} }
}''',
    BASE + "/network/ModNetwork.java": '''
package net.onixary.shapeShifterCurseForge.network;
import net.minecraft.server.level.ServerPlayer;
public class ModNetwork {
 public static final java.util.List<PowerAnimationPacket> sent = new java.util.ArrayList<>();
 public static void sendPowerAnimation(ServerPlayer p,PowerAnimationPacket packet){sent.add(packet);}
 public static void sendPowerAnimationTo(ServerPlayer p,ServerPlayer r,PowerAnimationPacket packet){sent.add(packet);}
}''',
    BASE + "/client/render/FormAnimationSystem.java": '''
package net.onixary.shapeShifterCurseForge.client.render;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
public class FormAnimationSystem {
 public static float speed=1;
 public record Selection(ResourceLocation id,float speed){}
 public static Selection powerSelection(Player p,ResourceLocation id){return new Selection(id,speed);}
}''',
    BASE + "/client/render/BedrockAnimationPlayer.java": '''
package net.onixary.shapeShifterCurseForge.client.render;
public class BedrockAnimationPlayer {
 public static float animationLength(FormAnimationSystem.Selection s){return 1;}
}''',
}

STUBS.update({
    "net/minecraft/util/Mth.java": "package net.minecraft.util; public class Mth { public static float lerp(float a,float x,float y){return x+(y-x)*a;} }",
    "net/minecraft/client/model/geom/ModelPart.java": """
package net.minecraft.client.model.geom;
public class ModelPart {
 public float x,y,z,xRot,yRot,zRot;
 public void copyFrom(ModelPart p){x=p.x;y=p.y;z=p.z;xRot=p.xRot;yRot=p.yRot;zRot=p.zRot;}
}""",
    "net/minecraft/client/model/PlayerModel.java": """
package net.minecraft.client.model;
import net.minecraft.client.model.geom.ModelPart;
public class PlayerModel<T> {
 public final ModelPart head=new ModelPart(),hat=new ModelPart(),body=new ModelPart(),
 rightArm=new ModelPart(),leftArm=new ModelPart(),rightLeg=new ModelPart(),leftLeg=new ModelPart();
}""",
})

TEST = '''
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.model.PlayerModel;
import net.onixary.shapeShifterCurseForge.client.render.PlayerModelPose;
import net.onixary.shapeShifterCurseForge.animation.*;
import net.onixary.shapeShifterCurseForge.network.*;
import net.onixary.shapeShifterCurseForge.power.PowerAnimationService;
import net.onixary.shapeShifterCurseForge.client.PowerAnimationClientHandler;
import net.onixary.shapeShifterCurseForge.client.render.FormAnimationSystem;
import net.onixary.shapeShifterCurseForge.client.render.AnimationProfile;

public class AnimationRegression {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void near(float a,float b,String message){check(Math.abs(a-b)<.0001,message+": "+a+" != "+b);}
 static PowerAnimationPacket last(){return ModNetwork.sent.get(ModNetwork.sent.size()-1);}
 static void receive(PowerAnimationPacket p){PowerAnimationClientHandler.apply(p.forEntity(7));}
 public static void main(String[] args){
  var id=ResourceLocation.tryParse("ssc:attack");
  var other=ResourceLocation.tryParse("ssc:other");
  var server=new ServerPlayer(); var receiver=new ServerPlayer();
  var world=new ClientLevel(); Minecraft.getInstance().level=world;
  var player=new Player(7,world);

  PowerAnimationService.playLoop(server,id);
  check(last().durationOrCount()==120 && !last().refresh(),"loop starts with finite lease");
  receive(last()); world.now=99;
  var initial=PowerAnimationClientHandler.active(player,0);
  near(initial.timeSeconds(),4.95f,"world clock advances loop");
  server.tickCount=100; PowerAnimationService.tick(server);
  check(last().refresh() && last().durationOrCount()==120,"100 tick heartbeat");
  world.now=100; receive(last());
  check(PowerAnimationClientHandler.active(player,0).clock()==initial.clock(),"renewal preserves playback identity");
  near(PowerAnimationClientHandler.active(player,0).timeSeconds(),5,"renewal does not restart");
  world.now=219; check(PowerAnimationClientHandler.active(player,0)!=null,"lease active before deadline");
  world.now=220; check(PowerAnimationClientHandler.active(player,0)==null,"missing STOP/renewal expires");

  PowerAnimationService.synchronizeTo(server,receiver); receive(last());
  check(PowerAnimationClientHandler.active(player,0)!=null,"tracking recovers persistent loop");
  PowerAnimationService.playWithCount(server,id,2); receive(last());
  ModNetwork.sent.clear(); PowerAnimationService.synchronizeTo(server,receiver);
  check(ModNetwork.sent.isEmpty(),"COUNT replaces persistent LOOP and is not replayed for new trackers");
  PowerAnimationService.stop(server); check(last().mode()==PowerAnimationPacket.Mode.STOP,"COUNT can be explicitly stopped");
  receive(last()); check(PowerAnimationClientHandler.active(player,0)==null,"STOP reaches client-owned COUNT");

  PowerAnimationService.playWithCount(server,id,2); receive(last());
  PowerAnimationService.stopIfMatches(server,other); receive(last());
  check(PowerAnimationClientHandler.active(player,0)!=null,"conditional STOP preserves different clip");
  PowerAnimationService.stopIfMatches(server,id); receive(last());
  check(PowerAnimationClientHandler.active(player,0)==null,"conditional STOP cancels COUNT");

  FormAnimationSystem.speed=2; PowerAnimationService.playWithCount(server,id,2); receive(last());
  world.now=239; check(PowerAnimationClientHandler.active(player,0)!=null,"COUNT respects playback speed before end");
  world.now=240; check(PowerAnimationClientHandler.active(player,0)==null,"COUNT ends after N speed-adjusted cycles");
  FormAnimationSystem.speed=1;

  PowerAnimationService.playWithTime(server,id,150); receive(last());
  for(int t=1;t<=100;t++){server.tickCount=t;PowerAnimationService.tick(server);}
  check(last().refresh() && last().durationOrCount()==50,"TIME heartbeat sends remaining duration");
  world.now=340; receive(last());
  near(PowerAnimationClientHandler.active(player,0).timeSeconds(),5,"TIME heartbeat preserves phase");
  for(int t=101;t<=150;t++){server.tickCount=t;PowerAnimationService.tick(server);}
  check(last().mode()==PowerAnimationPacket.Mode.STOP,"TIME sends STOP on expiry");
  world.now=390; check(PowerAnimationClientHandler.active(player,0)==null,"TIME also expires without STOP delivery");

  PowerAnimationService.playLoop(server,id); receive(last());
  world.now=400; PowerAnimationService.playLoop(server,id); receive(last());
  near(PowerAnimationClientHandler.active(player,0).timeSeconds(),0,"explicit same-id start restarts");
  PowerAnimationClientHandler.remove(7);
  check(PowerAnimationClientHandler.active(new Player(7,world),0)==null,"entity removal clears reused id");
  PowerAnimationService.synchronizeTo(server,receiver); receive(last());
  check(PowerAnimationClientHandler.active(new Player(7,world),0)!=null,"entity recreation recovers by request");
  Minecraft.getInstance().level=new ClientLevel(); PowerAnimationClientHandler.ensureLevel(Minecraft.getInstance().level);
  check(PowerAnimationClientHandler.active(new Player(7,Minecraft.getInstance().level),0)==null,"world change clears old lease");
  PowerAnimationService.forget(server); ModNetwork.sent.clear(); PowerAnimationService.synchronizeTo(server,receiver);
  check(ModNetwork.sent.isEmpty(),"logout forgets persistent animation");

  for(var packet:java.util.List.of(PowerAnimationPacket.start(id,PowerAnimationPacket.Mode.LOOP,120).asRefresh(),
       PowerAnimationPacket.stopMatching(id),PowerAnimationPacket.stop())){
   var buffer=new FriendlyByteBuf(); PowerAnimationPacket.encode(packet,buffer);
   check(packet.equals(PowerAnimationPacket.decode(buffer)),"packet field round trip");
  }
  near(AnimationTransition.rotationDegrees(170,-170,.5f),180,"degree bones take shortest arc");
  near(AnimationTransition.rotation(0,(float)Math.toRadians(201.62),.5f),(float)Math.toRadians(-79.19),"radian body takes shortest arc");
  near(AnimationTransition.DEFAULT.blend(5,10),.5f,"fade tick duration");
  near(new AnimationTransition(AnimationTransition.Easing.IN_QUAD,false).blend(5,10),.25f,"transition easing");
  near(new AnimationTransition(AnimationTransition.Easing.IN_QUAD,true).blend(0,10),1,"skipFade bypasses interpolation");
  var transition=new AnimationTransition(AnimationTransition.Easing.OUT_CUBIC,true);
  var profile=AnimationProfile.builder().animation("action","file","clip",2,7,transition).build();
  check(profile.get("action").transition().equals(transition),"profile retains transition metadata");
  for(var easing:AnimationTransition.Easing.values()){
   near(easing.apply(0),0,"easing start "+easing); near(easing.apply(1),1,"easing end "+easing);
  }
  var model=new PlayerModel<>();
  model.head.xRot=.7f; model.body.xRot=1.2f; model.leftArm.y=8; model.rightLeg.z=6;
  var pose=PlayerModelPose.capture(model);
  // Vanilla setupAnim and an equipment model begin from different intermediate poses.
  model.head.xRot=0; model.body.xRot=0; model.leftArm.y=2; model.rightLeg.z=0;
  pose.apply(model);
  check(pose.equals(PlayerModelPose.capture(model)),"vanilla pass restores exactly the Geo pre-pass pose");
  near(model.hat.xRot,.7f,"hat follows animated head after vanilla copied it too early");
  var equipment=new PlayerModel<>(); equipment.body.xRot=-2;
  pose.apply(equipment);
  check(pose.equals(PlayerModelPose.capture(equipment)),"equipment gets same pose independent of baseline");
  var zero=new PlayerModel<>(); var halfway=PlayerModelPose.lerp(PlayerModelPose.capture(zero),pose,.5f);
  halfway.apply(model); halfway.apply(model);
  check(halfway.equals(PlayerModelPose.capture(model)),"repeated render passes do not accumulate fade offsets");
  near(model.leftArm.y,4,"fade pivot remains identical in repeated equipment pass");
  System.out.println("Passed "+checks+" animation lifecycle/transition checks.");
 }
}
'''

def main():
    javac = shutil.which("javac")
    if not javac:
        raise SystemExit("JDK 17+ javac is required on PATH")
    java = str(Path(javac).with_name("java.exe" if Path(javac).suffix == ".exe" else "java"))
    with tempfile.TemporaryDirectory(prefix="ssc-animation-tests-") as folder:
        temp = Path(folder)
        for name, code in STUBS.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(code, encoding="utf-8")
        (temp / "AnimationRegression.java").write_text(TEST, encoding="utf-8")
        production = [ROOT / "src/main/java" / BASE / name for name in (
            "animation/PlaybackClock.java", "animation/AnimationTransition.java",
            "power/PowerAnimationService.java", "client/PowerAnimationClientHandler.java",
            "network/PowerAnimationPacket.java", "client/render/AnimationProfile.java",
            "client/render/PlayerModelPose.java")]
        files = list(temp.rglob("*.java")) + production
        subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "-d", str(temp / "classes"),
                        *map(str, files)], check=True)
        subprocess.run([java, "-cp", str(temp / "classes"), "AnimationRegression"], check=True)

if __name__ == "__main__":
    main()
