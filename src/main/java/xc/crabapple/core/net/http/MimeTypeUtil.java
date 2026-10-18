package xc.crabapple.core.net.http;

import xc.crabapple.core.collection.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 2016-12-07 1.0.0
 * 获取Content-Type(Mime-Type)工具类
 */
public final class MimeTypeUtil {

    //默认mime
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static Map<String, String> p_mimeMap = null;

    /**
     * 2016-12-07 1.0.0
     * 2023-05-19 补充大量数据
     */
    public static final void init(){
        p_mimeMap = new HashMap<>();
        p_mimeMap.put("htx", "text/html");
        p_mimeMap.put("img", "application/x-img");
        p_mimeMap.put("java", "java/*");
        p_mimeMap.put("jsp", "text/html");
        p_mimeMap.put("apk", "application/vnd.android.package-archive");
        p_mimeMap.put("m4a", "audio/mp4");
        p_mimeMap.put("ogg", "audio/ogg");
        p_mimeMap.put("oga", "audio/ogg");
        p_mimeMap.put("webma", "audio/webm");
        p_mimeMap.put("m4v", "video/mp4");
        p_mimeMap.put("ogv", "video/ogg");
        p_mimeMap.put("webm", "video/webm");
        p_mimeMap.put("webmv", "video/webm");
        // 2023-05-19 supply
        p_mimeMap.put("123", "application/vnd.lotus-1-2-3");
        p_mimeMap.put("3gp", "video/3gpp");
        p_mimeMap.put("aab", "application/x-authoware-bin");
        p_mimeMap.put("aam", "application/x-authoware-map");
        p_mimeMap.put("aas", "application/x-authoware-seg");
        p_mimeMap.put("ai", "application/postscript");
        p_mimeMap.put("aif", "audio/x-aiff");
        p_mimeMap.put("aifc", "audio/x-aiff");
        p_mimeMap.put("aiff", "audio/x-aiff");
        p_mimeMap.put("als", "audio/X-Alpha5");
        p_mimeMap.put("amc", "application/x-mpeg");
        p_mimeMap.put("ani", "application/octet-stream");
        p_mimeMap.put("asc", "text/plain");
        p_mimeMap.put("asd", "application/astound");
        p_mimeMap.put("asf", "video/x-ms-asf");
        p_mimeMap.put("asn", "application/astound");
        p_mimeMap.put("asp", "application/x-asap");
        p_mimeMap.put("asx", "video/x-ms-asf");
        p_mimeMap.put("au", "audio/basic");
        p_mimeMap.put("avb", "application/octet-stream");
        p_mimeMap.put("avi", "video/x-msvideo");
        p_mimeMap.put("awb", "audio/amr-wb");
        p_mimeMap.put("bcpio", "application/x-bcpio");
        p_mimeMap.put("bin", "application/octet-stream");
        p_mimeMap.put("bld", "application/bld");
        p_mimeMap.put("bld2", "application/bld2");
        p_mimeMap.put("bmp", "application/x-MS-bmp");
        p_mimeMap.put("bpk", "application/octet-stream");
        p_mimeMap.put("bz2", "application/x-bzip2");
        p_mimeMap.put("cal", "image/x-cals");
        p_mimeMap.put("ccn", "application/x-cnc");
        p_mimeMap.put("cco", "application/x-cocoa");
        p_mimeMap.put("cdf", "application/x-netcdf");
        p_mimeMap.put("cgi", "magnus-internal/cgi");
        p_mimeMap.put("chat", "application/x-chat");
        p_mimeMap.put("class", "application/octet-stream");
        p_mimeMap.put("clp", "application/x-msclip");
        p_mimeMap.put("cmx", "application/x-cmx");
        p_mimeMap.put("co", "application/x-cult3d-object");
        p_mimeMap.put("cod", "image/cis-cod");
        p_mimeMap.put("cpio", "application/x-cpio");
        p_mimeMap.put("cpt", "application/mac-compactpro");
        p_mimeMap.put("crd", "application/x-mscardfile");
        p_mimeMap.put("csh", "application/x-csh");
        p_mimeMap.put("csm", "chemical/x-csml");
        p_mimeMap.put("csml", "chemical/x-csml");
        p_mimeMap.put("css", "text/css");
        p_mimeMap.put("cur", "application/octet-stream");
        p_mimeMap.put("dcm", "x-lml/x-evm");
        p_mimeMap.put("dcr", "application/x-director");
        p_mimeMap.put("dcx", "image/x-dcx");
        p_mimeMap.put("dhtml", "text/html");
        p_mimeMap.put("dir", "application/x-director");
        p_mimeMap.put("dll", "application/octet-stream");
        p_mimeMap.put("dmg", "application/octet-stream");
        p_mimeMap.put("dms", "application/octet-stream");
        p_mimeMap.put("doc", "application/msword");
        p_mimeMap.put("docx", "application/msword");
        p_mimeMap.put("dot", "application/x-dot");
        p_mimeMap.put("dvi", "application/x-dvi");
        p_mimeMap.put("dwf", "drawing/x-dwf");
        p_mimeMap.put("dwg", "application/x-autocad");
        p_mimeMap.put("dxf", "application/x-autocad");
        p_mimeMap.put("dxr", "application/x-director");
        p_mimeMap.put("ebk", "application/x-expandedbook");
        p_mimeMap.put("emb", "chemical/x-embl-dl-nucleotide");
        p_mimeMap.put("embl", "chemical/x-embl-dl-nucleotide");
        p_mimeMap.put("eps", "application/postscript");
        p_mimeMap.put("eri", "image/x-eri");
        p_mimeMap.put("es", "audio/echospeech");
        p_mimeMap.put("esl", "audio/echospeech");
        p_mimeMap.put("etc", "application/x-earthtime");
        p_mimeMap.put("etx", "text/x-setext");
        p_mimeMap.put("evm", "x-lml/x-evm");
        p_mimeMap.put("evy", "application/x-envoy");
        p_mimeMap.put("exe", "application/octet-stream");
        p_mimeMap.put("fh4", "image/x-freehand");
        p_mimeMap.put("fh5", "image/x-freehand");
        p_mimeMap.put("fhc", "image/x-freehand");
        p_mimeMap.put("fif", "image/fif");
        p_mimeMap.put("fm", "application/x-maker");
        p_mimeMap.put("fpx", "image/x-fpx");
        p_mimeMap.put("fvi", "video/isivideo");
        p_mimeMap.put("gau", "chemical/x-gaussian-input");
        p_mimeMap.put("gca", "application/x-gca-compressed");
        p_mimeMap.put("gdb", "x-lml/x-gdb");
        p_mimeMap.put("gif", "image/gif");
        p_mimeMap.put("gps", "application/x-gps");
        p_mimeMap.put("gtar", "application/x-gtar");
        p_mimeMap.put("gz", "application/x-gzip");
        p_mimeMap.put("hdf", "application/x-hdf");
        p_mimeMap.put("hdm", "text/x-hdml");
        p_mimeMap.put("hdml", "text/x-hdml");
        p_mimeMap.put("hlp", "application/winhlp");
        p_mimeMap.put("hqx", "application/mac-binhex40");
        p_mimeMap.put("htm", "text/html");
        p_mimeMap.put("html", "text/html");
        p_mimeMap.put("hts", "text/html");
        p_mimeMap.put("ice", "x-conference/x-cooltalk");
        p_mimeMap.put("ico", "application/octet-stream");
        p_mimeMap.put("ief", "image/ief");
        p_mimeMap.put("ifm", "image/gif");
        p_mimeMap.put("ifs", "image/ifs");
        p_mimeMap.put("imy", "audio/melody");
        p_mimeMap.put("ins", "application/x-NET-Install");
        p_mimeMap.put("ips", "application/x-ipscript");
        p_mimeMap.put("ipx", "application/x-ipix");
        p_mimeMap.put("it", "audio/x-mod");
        p_mimeMap.put("itz", "audio/x-mod");
        p_mimeMap.put("ivr", "i-world/i-vrml");
        p_mimeMap.put("j2k", "image/j2k");
        p_mimeMap.put("jad", "text/vnd.sun.j2me.app-descriptor");
        p_mimeMap.put("jam", "application/x-jam");
        p_mimeMap.put("jar", "application/java-archive");
        p_mimeMap.put("jnlp", "application/x-java-jnlp-file");
        p_mimeMap.put("jpe", "image/jpeg");
        p_mimeMap.put("jpeg", "image/jpeg");
        p_mimeMap.put("jpg", "image/jpeg");
        p_mimeMap.put("jpz", "image/jpeg");
        p_mimeMap.put("js", "application/x-javascript");
        p_mimeMap.put("jwc", "application/jwc");
        p_mimeMap.put("kjx", "application/x-kjx");
        p_mimeMap.put("lak", "x-lml/x-lak");
        p_mimeMap.put("latex", "application/x-latex");
        p_mimeMap.put("lcc", "application/fastman");
        p_mimeMap.put("lcl", "application/x-digitalloca");
        p_mimeMap.put("lcr", "application/x-digitalloca");
        p_mimeMap.put("lgh", "application/lgh");
        p_mimeMap.put("lha", "application/octet-stream");
        p_mimeMap.put("lml", "x-lml/x-lml");
        p_mimeMap.put("lmlpack", "x-lml/x-lmlpack");
        p_mimeMap.put("lsf", "video/x-ms-asf");
        p_mimeMap.put("lsx", "video/x-ms-asf");
        p_mimeMap.put("lzh", "application/x-lzh");
        p_mimeMap.put("m13", "application/x-msmediaview");
        p_mimeMap.put("m14", "application/x-msmediaview");
        p_mimeMap.put("m15", "audio/x-mod");
        p_mimeMap.put("m3u", "audio/x-mpegurl");
        p_mimeMap.put("m3url", "audio/x-mpegurl");
        p_mimeMap.put("ma1", "audio/ma1");
        p_mimeMap.put("ma2", "audio/ma2");
        p_mimeMap.put("ma3", "audio/ma3");
        p_mimeMap.put("ma5", "audio/ma5");
        p_mimeMap.put("man", "application/x-troff-man");
        p_mimeMap.put("map", "magnus-internal/imagemap");
        p_mimeMap.put("mbd", "application/mbedlet");
        p_mimeMap.put("mct", "application/x-mascot");
        p_mimeMap.put("mdb", "application/x-msaccess");
        p_mimeMap.put("mdz", "audio/x-mod");
        p_mimeMap.put("me", "application/x-troff-me");
        p_mimeMap.put("mel", "text/x-vmel");
        p_mimeMap.put("mi", "application/x-mif");
        p_mimeMap.put("mid", "audio/midi");
        p_mimeMap.put("midi", "audio/midi");
        p_mimeMap.put("mif", "application/x-mif");
        p_mimeMap.put("mil", "image/x-cals");
        p_mimeMap.put("mio", "audio/x-mio");
        p_mimeMap.put("mmf", "application/x-skt-lbs");
        p_mimeMap.put("mng", "video/x-mng");
        p_mimeMap.put("mny", "application/x-msmoney");
        p_mimeMap.put("moc", "application/x-mocha");
        p_mimeMap.put("mocha", "application/x-mocha");
        p_mimeMap.put("mod", "audio/x-mod");
        p_mimeMap.put("mof", "application/x-yumekara");
        p_mimeMap.put("mol", "chemical/x-mdl-molfile");
        p_mimeMap.put("mop", "chemical/x-mopac-input");
        p_mimeMap.put("mov", "video/quicktime");
        p_mimeMap.put("movie", "video/x-sgi-movie");
        p_mimeMap.put("mp2", "audio/x-mpeg");
        p_mimeMap.put("mp3", "audio/x-mpeg");
        p_mimeMap.put("mp4", "video/mp4");
        p_mimeMap.put("mpc", "application/vnd.mpohun.certificate");
        p_mimeMap.put("mpe", "video/mpeg");
        p_mimeMap.put("mpeg", "video/mpeg");
        p_mimeMap.put("mpg", "video/mpeg");
        p_mimeMap.put("mpg4", "video/mp4");
        p_mimeMap.put("mpga", "audio/mpeg");
        p_mimeMap.put("mpn", "application/vnd.mophun.application");
        p_mimeMap.put("mpp", "application/vnd.ms-project");
        p_mimeMap.put("mps", "application/x-mapserver");
        p_mimeMap.put("mrl", "text/x-mrml");
        p_mimeMap.put("mrm", "application/x-mrm");
        p_mimeMap.put("ms", "application/x-troff-ms");
        p_mimeMap.put("mts", "application/metastream");
        p_mimeMap.put("mtx", "application/metastream");
        p_mimeMap.put("mtz", "application/metastream");
        p_mimeMap.put("mzv", "application/metastream");
        p_mimeMap.put("nar", "application/zip");
        p_mimeMap.put("nbmp", "image/nbmp");
        p_mimeMap.put("nc", "application/x-netcdf");
        p_mimeMap.put("ndb", "x-lml/x-ndb");
        p_mimeMap.put("ndwn", "application/ndwn");
        p_mimeMap.put("nif", "application/x-nif");
        p_mimeMap.put("nmz", "application/x-scream");
        p_mimeMap.put("nokia-op-logo", "image/vnd.nok-oplogo-color");
        p_mimeMap.put("npx", "application/x-netfpx");
        p_mimeMap.put("nsnd", "audio/nsnd");
        p_mimeMap.put("nva", "application/x-neva1");
        p_mimeMap.put("oda", "application/oda");
        p_mimeMap.put("oom", "application/x-AtlasMate-Plugin");
        p_mimeMap.put("pac", "audio/x-pac");
        p_mimeMap.put("pae", "audio/x-epac");
        p_mimeMap.put("pan", "application/x-pan");
        p_mimeMap.put("pbm", "image/x-portable-bitmap");
        p_mimeMap.put("pcx", "image/x-pcx");
        p_mimeMap.put("pda", "image/x-pda");
        p_mimeMap.put("pdb", "chemical/x-pdb");
        p_mimeMap.put("pdf", "application/pdf");
        p_mimeMap.put("pfr", "application/font-tdpfr");
        p_mimeMap.put("pgm", "image/x-portable-graymap");
        p_mimeMap.put("pict", "image/x-pict");
        p_mimeMap.put("pm", "application/x-perl");
        p_mimeMap.put("pmd", "application/x-pmd");
        p_mimeMap.put("png", "image/png");
        p_mimeMap.put("pnm", "image/x-portable-anymap");
        p_mimeMap.put("pnz", "image/png");
        p_mimeMap.put("pot", "application/vnd.ms-powerpoint");
        p_mimeMap.put("ppm", "image/x-portable-pixmap");
        p_mimeMap.put("pps", "application/vnd.ms-powerpoint");
        p_mimeMap.put("ppt", "application/vnd.ms-powerpoint");
        p_mimeMap.put("pptx", "application/vnd.ms-powerpoint");
        p_mimeMap.put("pqf", "application/x-cprplayer");
        p_mimeMap.put("pqi", "application/cprplayer");
        p_mimeMap.put("prc", "application/x-prc");
        p_mimeMap.put("proxy", "application/x-ns-proxy-autoconfig");
        p_mimeMap.put("ps", "application/postscript");
        p_mimeMap.put("ptlk", "application/listenup");
        p_mimeMap.put("pub", "application/x-mspublisher");
        p_mimeMap.put("pvx", "video/x-pv-pvx");
        p_mimeMap.put("qcp", "audio/vnd.qcelp");
        p_mimeMap.put("qt", "video/quicktime");
        p_mimeMap.put("qti", "image/x-quicktime");
        p_mimeMap.put("qtif", "image/x-quicktime");
        p_mimeMap.put("r3t", "text/vnd.rn-realtext3d");
        p_mimeMap.put("ra", "audio/x-pn-realaudio");
        p_mimeMap.put("ram", "audio/x-pn-realaudio");
        p_mimeMap.put("rar", "application/x-rar-compressed");
        p_mimeMap.put("ras", "image/x-cmu-raster");
        p_mimeMap.put("rdf", "application/rdf+xml");
        p_mimeMap.put("rf", "image/vnd.rn-realflash");
        p_mimeMap.put("rgb", "image/x-rgb");
        p_mimeMap.put("rlf", "application/x-richlink");
        p_mimeMap.put("rm", "audio/x-pn-realaudio");
        p_mimeMap.put("rmf", "audio/x-rmf");
        p_mimeMap.put("rmm", "audio/x-pn-realaudio");
        p_mimeMap.put("rmvb", "audio/x-pn-realaudio");
        p_mimeMap.put("rnx", "application/vnd.rn-realplayer");
        p_mimeMap.put("roff", "application/x-troff");
        p_mimeMap.put("rp", "image/vnd.rn-realpix");
        p_mimeMap.put("rpm", "audio/x-pn-realaudio-plugin");
        p_mimeMap.put("rt", "text/vnd.rn-realtext");
        p_mimeMap.put("rte", "x-lml/x-gps");
        p_mimeMap.put("rtf", "application/rtf");
        p_mimeMap.put("rtg", "application/metastream");
        p_mimeMap.put("rtx", "text/richtext");
        p_mimeMap.put("rv", "video/vnd.rn-realvideo");
        p_mimeMap.put("rwc", "application/x-rogerwilco");
        p_mimeMap.put("s3m", "audio/x-mod");
        p_mimeMap.put("s3z", "audio/x-mod");
        p_mimeMap.put("sca", "application/x-supercard");
        p_mimeMap.put("scd", "application/x-msschedule");
        p_mimeMap.put("sdf", "application/e-score");
        p_mimeMap.put("sea", "application/x-stuffit");
        p_mimeMap.put("sgm", "text/x-sgml");
        p_mimeMap.put("sgml", "text/x-sgml");
        p_mimeMap.put("sh", "application/x-sh");
        p_mimeMap.put("shar", "application/x-shar");
        p_mimeMap.put("shtml", "magnus-internal/parsed-html");
        p_mimeMap.put("shw", "application/presentations");
        p_mimeMap.put("si6", "image/si6");
        p_mimeMap.put("si7", "image/vnd.stiwap.sis");
        p_mimeMap.put("si9", "image/vnd.lgtwap.sis");
        p_mimeMap.put("sis", "application/vnd.symbian.install");
        p_mimeMap.put("sit", "application/x-stuffit");
        p_mimeMap.put("skd", "application/x-Koan");
        p_mimeMap.put("skm", "application/x-Koan");
        p_mimeMap.put("skp", "application/x-Koan");
        p_mimeMap.put("skt", "application/x-Koan");
        p_mimeMap.put("slc", "application/x-salsa");
        p_mimeMap.put("smd", "audio/x-smd");
        p_mimeMap.put("smi", "application/smil");
        p_mimeMap.put("smil", "application/smil");
        p_mimeMap.put("smp", "application/studiom");
        p_mimeMap.put("smz", "audio/x-smd");
        p_mimeMap.put("snd", "audio/basic");
        p_mimeMap.put("spc", "text/x-speech");
        p_mimeMap.put("spl", "application/futuresplash");
        p_mimeMap.put("spr", "application/x-sprite");
        p_mimeMap.put("sprite", "application/x-sprite");
        p_mimeMap.put("spt", "application/x-spt");
        p_mimeMap.put("src", "application/x-wais-source");
        p_mimeMap.put("stk", "application/hyperstudio");
        p_mimeMap.put("stm", "audio/x-mod");
        p_mimeMap.put("sv4cpio", "application/x-sv4cpio");
        p_mimeMap.put("sv4crc", "application/x-sv4crc");
        p_mimeMap.put("svf", "image/vnd");
        p_mimeMap.put("svg", "image/svg-xml");
        p_mimeMap.put("svh", "image/svh");
        p_mimeMap.put("svr", "x-world/x-svr");
        p_mimeMap.put("swf", "application/x-shockwave-flash");
        p_mimeMap.put("swfl", "application/x-shockwave-flash");
        p_mimeMap.put("t", "application/x-troff");
        p_mimeMap.put("tad", "application/octet-stream");
        p_mimeMap.put("talk", "text/x-speech");
        p_mimeMap.put("tar", "application/x-tar");
        p_mimeMap.put("taz", "application/x-tar");
        p_mimeMap.put("tbp", "application/x-timbuktu");
        p_mimeMap.put("tbt", "application/x-timbuktu");
        p_mimeMap.put("tcl", "application/x-tcl");
        p_mimeMap.put("tex", "application/x-tex");
        p_mimeMap.put("texi", "application/x-texinfo");
        p_mimeMap.put("texinfo", "application/x-texinfo");
        p_mimeMap.put("tgz", "application/x-tar");
        p_mimeMap.put("thm", "application/vnd.eri.thm");
        p_mimeMap.put("tif", "image/tiff");
        p_mimeMap.put("tiff", "image/tiff");
        p_mimeMap.put("tki", "application/x-tkined");
        p_mimeMap.put("tkined", "application/x-tkined");
        p_mimeMap.put("toc", "application/toc");
        p_mimeMap.put("toy", "image/toy");
        p_mimeMap.put("tr", "application/x-troff");
        p_mimeMap.put("trk", "x-lml/x-gps");
        p_mimeMap.put("trm", "application/x-msterminal");
        p_mimeMap.put("tsi", "audio/tsplayer");
        p_mimeMap.put("tsp", "application/dsptype");
        p_mimeMap.put("tsv", "text/tab-separated-values");
        p_mimeMap.put("ttf", "application/octet-stream");
        p_mimeMap.put("ttz", "application/t-time");
        p_mimeMap.put("txt", "text/plain");
        p_mimeMap.put("ult", "audio/x-mod");
        p_mimeMap.put("ustar", "application/x-ustar");
        p_mimeMap.put("uu", "application/x-uuencode");
        p_mimeMap.put("uue", "application/x-uuencode");
        p_mimeMap.put("vcd", "application/x-cdlink");
        p_mimeMap.put("vcf", "text/x-vcard");
        p_mimeMap.put("vdo", "video/vdo");
        p_mimeMap.put("vib", "audio/vib");
        p_mimeMap.put("viv", "video/vivo");
        p_mimeMap.put("vivo", "video/vivo");
        p_mimeMap.put("vmd", "application/vocaltec-media-desc");
        p_mimeMap.put("vmf", "application/vocaltec-media-file");
        p_mimeMap.put("vmi", "application/x-dreamcast-vms-info");
        p_mimeMap.put("vms", "application/x-dreamcast-vms");
        p_mimeMap.put("vox", "audio/voxware");
        p_mimeMap.put("vqe", "audio/x-twinvq-plugin");
        p_mimeMap.put("vqf", "audio/x-twinvq");
        p_mimeMap.put("vql", "audio/x-twinvq");
        p_mimeMap.put("vre", "x-world/x-vream");
        p_mimeMap.put("vrml", "x-world/x-vrml");
        p_mimeMap.put("vrt", "x-world/x-vrt");
        p_mimeMap.put("vrw", "x-world/x-vream");
        p_mimeMap.put("vts", "workbook/formulaone");
        p_mimeMap.put("wav", "audio/x-wav");
        p_mimeMap.put("wax", "audio/x-ms-wax");
        p_mimeMap.put("wbmp", "image/vnd.wap.wbmp");
        p_mimeMap.put("web", "application/vnd.xara");
        p_mimeMap.put("wi", "image/wavelet");
        p_mimeMap.put("wis", "application/x-InstallShield");
        p_mimeMap.put("wm", "video/x-ms-wm");
        p_mimeMap.put("wma", "audio/x-ms-wma");
        p_mimeMap.put("wmd", "application/x-ms-wmd");
        p_mimeMap.put("wmf", "application/x-msmetafile");
        p_mimeMap.put("wml", "text/vnd.wap.wml");
        p_mimeMap.put("wmlc", "application/vnd.wap.wmlc");
        p_mimeMap.put("wmls", "text/vnd.wap.wmlscript");
        p_mimeMap.put("wmlsc", "application/vnd.wap.wmlscriptc");
        p_mimeMap.put("wmlscript", "text/vnd.wap.wmlscript");
        p_mimeMap.put("wmv", "audio/x-ms-wmv");
        p_mimeMap.put("wmx", "video/x-ms-wmx");
        p_mimeMap.put("wmz", "application/x-ms-wmz");
        p_mimeMap.put("wpng", "image/x-up-wpng");
        p_mimeMap.put("wpt", "x-lml/x-gps");
        p_mimeMap.put("wri", "application/x-mswrite");
        p_mimeMap.put("wrl", "x-world/x-vrml");
        p_mimeMap.put("wrz", "x-world/x-vrml");
        p_mimeMap.put("ws", "text/vnd.wap.wmlscript");
        p_mimeMap.put("wsc", "application/vnd.wap.wmlscriptc");
        p_mimeMap.put("wv", "video/wavelet");
        p_mimeMap.put("wvx", "video/x-ms-wvx");
        p_mimeMap.put("wxl", "application/x-wxl");
        p_mimeMap.put("x-gzip", "application/x-gzip");
        p_mimeMap.put("xar", "application/vnd.xara");
        p_mimeMap.put("xbm", "image/x-xbitmap");
        p_mimeMap.put("xdm", "application/x-xdma");
        p_mimeMap.put("xdma", "application/x-xdma");
        p_mimeMap.put("xdw", "application/vnd.fujixerox.docuworks");
        p_mimeMap.put("xht", "application/xhtml+xml");
        p_mimeMap.put("xhtm", "application/xhtml+xml");
        p_mimeMap.put("xhtml", "application/xhtml+xml");
        p_mimeMap.put("xla", "application/vnd.ms-excel");
        p_mimeMap.put("xlc", "application/vnd.ms-excel");
        p_mimeMap.put("xll", "application/x-excel");
        p_mimeMap.put("xlm", "application/vnd.ms-excel");
        p_mimeMap.put("xls", "application/vnd.ms-excel");
        p_mimeMap.put("xlsx", "application/vnd.ms-excel");
        p_mimeMap.put("xlt", "application/vnd.ms-excel");
        p_mimeMap.put("xlw", "application/vnd.ms-excel");
        p_mimeMap.put("xm", "audio/x-mod");
        p_mimeMap.put("xml", "text/xml");
        p_mimeMap.put("xmz", "audio/x-mod");
        p_mimeMap.put("xpi", "application/x-xpinstall");
        p_mimeMap.put("xpm", "image/x-xpixmap");
        p_mimeMap.put("xsit", "text/xml");
        p_mimeMap.put("xsl", "text/xml");
        p_mimeMap.put("xul", "text/xul");
        p_mimeMap.put("xwd", "image/x-xwindowdump");
        p_mimeMap.put("xyz", "chemical/x-pdb");
        p_mimeMap.put("yz1", "application/x-yz1");
        p_mimeMap.put("z", "application/x-compress");
        p_mimeMap.put("zac", "application/x-zaurus-zac");
        p_mimeMap.put("zip", "application/zip");
    }

    /**
     * 2016-12-07 1.0.0
     * 跟据文件名获取 Mime
     * @param fileName 文件名
     * @return mime
     */
    public static final String getMimeTypeByFileName(final String fileName){
        String ext = getContentType(fileName);
        return getMimeTypeByExt(ext);
    }

    /**
     * 2016-12-07 1.0.0
     * 跟据扩展名获取Mime
     * @param ext 扩展名
     * @return mime
     */
    public static final String getMimeTypeByExt(final String ext){
        String localMime = p_mimeMap.get(ext);
        if(StringUtil.isBlank(localMime)){
            localMime = DEFAULT_MIME_TYPE;
        }
        return localMime;
    }

    /**
     * 2016-12-07 1.0.0
     * 2023-01-11 代码整理
     * 跟据文件名获取文件类型（后缀）
     * @param fileName
     * @return contentType 文件类型
     */
    private static final String getContentType(final String fileName){
        if(StringUtil.isBlank(fileName)){
            return "";
        }
        int length = fileName.length();
        int dot = fileName.lastIndexOf('.');
        if(dot == -1){
            return "";
        }
        if(dot < (length - 1)){
            String type = fileName.substring(dot + 1);
            type = type.toLowerCase();
            return type;
        }
        return "";
    }

    public static final String getType(final String fileName){
        String localFileName = StringUtil.trim(fileName);
        if (StringUtil.isBlank(localFileName)){
            return "";
        }
        if(localFileName.indexOf(".") <= 0){
            return "";
        }
        //
        int index = localFileName.indexOf('.') + 1;
        String suffix = localFileName.substring(index);
        suffix = StringUtil.toLowerCase(suffix);
        String type = p_mimeMap.get(suffix);
        if(StringUtil.isBlank(type)){
            return "";
        }
        //
        return type;
    }

    /**
     * 2022-06-28 add
     * 直接提取type值
     * @param fileName
     * @return
     */
    public static final String getTypePure(final String fileName){
        String localFileName = StringUtil.trim(fileName);
        if(StringUtil.isBlank(localFileName)){
            return "";
        }
        localFileName = StringUtil.toLowerCase(localFileName);
        String type = p_mimeMap.get(localFileName);
        if(StringUtil.isBlank(type)){
            return "";
        }
        return type;
    }

}