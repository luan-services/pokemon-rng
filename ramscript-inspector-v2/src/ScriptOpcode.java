final class ScriptOpcode {
    enum OperandFormat {
        NONE(0), U8(1), U16(2), PTR(4), U8_U8(2), U8_U16(3), U8_U32(5),
        U16_U16(4), COND_U8(2), COND_PTR(5), U8_PTR(5), PTR_U8(5), PTR_PTR(8);

        private final int size;

        OperandFormat(int size) {
            this.size = size;
        }

        int size() {
            return size;
        }
    }

    private static final String[] NAMES = {
        "nop",
        "nop1",
        "end",
        "return",
        "call",
        "goto",
        "goto_if",
        "call_if",
        "gotostd",
        "callstd",
        "gotostd_if",
        "callstd_if",
        "returnram",
        "endram",
        "setmysteryeventstatus",
        "loadword",
        "loadbyte",
        "setptr",
        "loadbytefromptr",
        "setptrbyte",
        "copylocal",
        "copybyte",
        "setvar",
        "addvar",
        "subvar",
        "copyvar",
        "setorcopyvar",
        "compare_local_to_local",
        "compare_local_to_value",
        "compare_local_to_ptr",
        "compare_ptr_to_local",
        "compare_ptr_to_value",
        "compare_ptr_to_ptr",
        "compare_var_to_value",
        "compare_var_to_var",
        "callnative",
        "gotonative",
        "special",
        "specialvar",
        "waitstate",
        "delay",
        "setflag",
        "clearflag",
        "checkflag",
        "initclock",
        "dotimebasedevents",
        "gettime",
        "playse",
        "waitse",
        "playfanfare",
        "waitfanfare",
        "playbgm",
        "savebgm",
        "fadedefaultbgm",
        "fadenewbgm",
        "fadeoutbgm",
        "fadeinbgm",
        "warp",
        "warpsilent",
        "warpdoor",
        "warphole",
        "warpteleport",
        "setwarp",
        "setdynamicwarp",
        "setdivewarp",
        "setholewarp",
        "getplayerxy",
        "getpartysize",
        "additem",
        "removeitem",
        "checkitemspace",
        "checkitem",
        "checkitemtype",
        "addpcitem",
        "checkpcitem",
        "adddecoration",
        "removedecoration",
        "checkdecor",
        "checkdecorspace",
        "applymovement",
        "applymovementat",
        "waitmovement",
        "waitmovementat",
        "removeobject",
        "removeobjectat",
        "addobject",
        "addobjectat",
        "setobjectxy",
        "showobjectat",
        "hideobjectat",
        "faceplayer",
        "turnobject",
        "trainerbattle",
        "dotrainerbattle",
        "gotopostbattlescript",
        "gotobeatenscript",
        "checktrainerflag",
        "settrainerflag",
        "cleartrainerflag",
        "setobjectxyperm",
        "copyobjectxytoperm",
        "setobjectmovementtype",
        "waitmessage",
        "message",
        "closemessage",
        "lockall",
        "lock",
        "releaseall",
        "release",
        "waitbuttonpress",
        "yesnobox",
        "multichoice",
        "multichoicedefault",
        "multichoicegrid",
        "drawbox",
        "erasebox",
        "drawboxtext",
        "showmonpic",
        "hidemonpic",
        "showcontestpainting",
        "braillemessage",
        "givemon",
        "giveegg",
        "setmonmove",
        "checkpartymove",
        "bufferspeciesname",
        "bufferleadmonspeciesname",
        "bufferpartymonnick",
        "bufferitemname",
        "bufferdecorationname",
        "buffermovename",
        "buffernumberstring",
        "bufferstdstring",
        "bufferstring",
        "pokemart",
        "pokemartdecoration",
        "pokemartdecoration2",
        "playslotmachine",
        "setberrytree",
        "choosecontestmon",
        "startcontest",
        "showcontestresults",
        "contestlinktransfer",
        "random",
        "addmoney",
        "removemoney",
        "checkmoney",
        "showmoneybox",
        "hidemoneybox",
        "updatemoneybox",
        "getpokenewsactive",
        "fadescreen",
        "fadescreenspeed",
        "setflashlevel",
        "animateflash",
        "messageautoscroll",
        "dofieldeffect",
        "setfieldeffectargument",
        "waitfieldeffect",
        "setrespawn",
        "checkplayergender",
        "playmoncry",
        "setmetatile",
        "resetweather",
        "setweather",
        "doweather",
        "setstepcallback",
        "setmaplayoutindex",
        "setobjectsubpriority",
        "resetobjectsubpriority",
        "createvobject",
        "turnvobject",
        "opendoor",
        "closedoor",
        "waitdooranim",
        "setdooropen",
        "setdoorclosed",
        "addelevmenuitem",
        "showelevmenu",
        "checkcoins",
        "addcoins",
        "removecoins",
        "setwildbattle",
        "dowildbattle",
        "setvaddress",
        "vgoto",
        "vcall",
        "vgoto_if",
        "vcall_if",
        "vmessage",
        "vbuffermessage",
        "vbufferstring",
        "showcoinsbox",
        "hidecoinsbox",
        "updatecoinsbox",
        "incrementgamestat",
        "setescapewarp",
        "waitmoncry",
        "bufferboxname",
        "textcolor",
        "loadhelp",
        "unloadhelp",
        "signmsg",
        "normalmsg",
        "comparestat",
        "setmonmodernfatefulencounter",
        "checkmonmodernfatefulencounter",
        "trywondercardscript",
        "setworldmapflag",
        "warpspinenter",
        "setmonmetlocation",
        "getbraillestringwidth",
        "bufferitemnameplural"
    };

    private static final OperandFormat[] FORMATS = new OperandFormat[256];

    static {
        FORMATS[0x00] = OperandFormat.NONE;
        FORMATS[0x01] = OperandFormat.NONE;
        FORMATS[0x02] = OperandFormat.NONE;
        FORMATS[0x03] = OperandFormat.NONE;
        FORMATS[0x04] = OperandFormat.PTR;
        FORMATS[0x05] = OperandFormat.PTR;
        FORMATS[0x06] = OperandFormat.COND_PTR;
        FORMATS[0x07] = OperandFormat.COND_PTR;
        FORMATS[0x08] = OperandFormat.U8;
        FORMATS[0x09] = OperandFormat.U8;
        FORMATS[0x0A] = OperandFormat.COND_U8;
        FORMATS[0x0B] = OperandFormat.COND_U8;
        FORMATS[0x0C] = OperandFormat.NONE;
        FORMATS[0x0D] = OperandFormat.NONE;
        FORMATS[0x0E] = OperandFormat.U8;
        FORMATS[0x0F] = OperandFormat.U8_U32;
        FORMATS[0x10] = OperandFormat.U8_U8;
        FORMATS[0x11] = OperandFormat.U8_U32;
        FORMATS[0x12] = OperandFormat.U8_U32;
        FORMATS[0x13] = OperandFormat.U8_U32;
        FORMATS[0x14] = OperandFormat.U8_U8;
        FORMATS[0x15] = OperandFormat.PTR_PTR;
        FORMATS[0x16] = OperandFormat.U16_U16;
        FORMATS[0x17] = OperandFormat.U16_U16;
        FORMATS[0x18] = OperandFormat.U16_U16;
        FORMATS[0x19] = OperandFormat.U16_U16;
        FORMATS[0x1A] = OperandFormat.U16_U16;
        FORMATS[0x1B] = OperandFormat.U8_U8;
        FORMATS[0x1C] = OperandFormat.U8_U8;
        FORMATS[0x1D] = OperandFormat.U8_PTR;
        FORMATS[0x1E] = OperandFormat.PTR_U8;
        FORMATS[0x1F] = OperandFormat.PTR_U8;
        FORMATS[0x20] = OperandFormat.PTR_PTR;
        FORMATS[0x21] = OperandFormat.U16_U16;
        FORMATS[0x22] = OperandFormat.U16_U16;
        FORMATS[0x23] = OperandFormat.PTR;
        FORMATS[0x24] = OperandFormat.PTR;
        FORMATS[0x25] = OperandFormat.U16;
        FORMATS[0x26] = OperandFormat.U16_U16;
        FORMATS[0x27] = OperandFormat.NONE;
        FORMATS[0x28] = OperandFormat.U16;
        FORMATS[0x29] = OperandFormat.U16;
        FORMATS[0x2A] = OperandFormat.U16;
        FORMATS[0x2B] = OperandFormat.U16;
        FORMATS[0x2F] = OperandFormat.U16;
        FORMATS[0x30] = OperandFormat.NONE;
        FORMATS[0x31] = OperandFormat.U16;
        FORMATS[0x32] = OperandFormat.NONE;
        FORMATS[0x44] = OperandFormat.U16_U16;
        FORMATS[0x45] = OperandFormat.U16_U16;
        FORMATS[0x46] = OperandFormat.U16_U16;
        FORMATS[0x47] = OperandFormat.U16_U16;
        FORMATS[0x48] = OperandFormat.U16;
        FORMATS[0x49] = OperandFormat.U16_U16;
        FORMATS[0x4A] = OperandFormat.U16_U16;
        FORMATS[0x5A] = OperandFormat.NONE;
        FORMATS[0x66] = OperandFormat.NONE;
        FORMATS[0x67] = OperandFormat.PTR;
        FORMATS[0x68] = OperandFormat.NONE;
        FORMATS[0x69] = OperandFormat.NONE;
        FORMATS[0x6A] = OperandFormat.NONE;
        FORMATS[0x6B] = OperandFormat.NONE;
        FORMATS[0x6C] = OperandFormat.NONE;
        FORMATS[0x6D] = OperandFormat.NONE;
        FORMATS[0x7D] = OperandFormat.U8_U16;
        FORMATS[0x80] = OperandFormat.U8_U16;
        FORMATS[0x83] = OperandFormat.U8_U16;
        FORMATS[0x84] = OperandFormat.U8_U16;
        FORMATS[0x85] = OperandFormat.U8_PTR;
        FORMATS[0x8F] = OperandFormat.U16;
        FORMATS[0xB8] = OperandFormat.PTR;
        FORMATS[0xB9] = OperandFormat.PTR;
        FORMATS[0xBA] = OperandFormat.PTR;
        FORMATS[0xBB] = OperandFormat.COND_PTR;
        FORMATS[0xBC] = OperandFormat.COND_PTR;
        FORMATS[0xBD] = OperandFormat.PTR;
        FORMATS[0xBE] = OperandFormat.PTR;
        FORMATS[0xBF] = OperandFormat.U8_PTR;
        FORMATS[0xC3] = OperandFormat.U8;
        FORMATS[0xC7] = OperandFormat.U8;
        FORMATS[0xCF] = OperandFormat.NONE;
    }

    private ScriptOpcode() {}

    static String name(int opcode) {
        if (opcode >= 0 && opcode < NAMES.length) {
            return NAMES[opcode];
        }
        return "unknown";
    }

    static OperandFormat format(int opcode) {
        return FORMATS[opcode & 0xFF];
    }
}
