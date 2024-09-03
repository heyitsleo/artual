# Claim / Raid system for ArtualCM

Plugin should do the following:

Claiming Chunks: Players can use the /claim command to claim chunks, with the maximum number of claims set in a configuration file.

Breaking Blocks in Claimed Chunks:

    If an untrusted player tries to break a block in a claimed chunk, it will take a long time to break. This is achieved by temporarily replacing the block with reinforced deepslate and giving the player Haste I.
    Breaking this reinforced deepslate block starts a raid.

Raid Mechanics:

    When a raid starts, no blocks can be placed in the claimed chunk by either the raiding player or the claim owner for the next 20 minutes.
    The owner of the claimed chunk is alerted when a raid starts.
    A raid cannot start, and the reinforced deepslate cannot be broken, if the owner of the claim is not online.

Honeycomb Blocks:

    Any blocks broken during a raid are replaced with honeycomb, which has Mining Fatigue II.
    Breaking a honeycomb block resets the raid timer back to 20 minutes.
