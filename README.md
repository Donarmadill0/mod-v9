# God Voice - client-side Fabric mod (1.21.11)

ONE jar, goes in .minecraft/mods (needs Fabric Loader + Fabric API for 1.21.11). Nothing on the server.
Only players with the mod see the dialogue box.

## Be a god
/god name Sirius          pick your name
/god color #D6A244        pick your colour
/god preview Be safe.     see it on your own screen
/god speak <player> <message>
/god follower add <player>, /god broadcast <message>

## Be a follower
/vow swear <god>, /vow break <god>, /vow list

How it works: your message is sent as a normal private message starting with a tag like
[GV|D6A244|SIRIUS]. A client with the mod hides that line and shows the box. Players without the mod see the plain whisper.
(Any chat/tellraw message that starts with such a tag is shown as dialogue too.)

Look: constants at the top of src/main/java/com/godvoice/DialogueOverlay.java
