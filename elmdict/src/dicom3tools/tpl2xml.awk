BEGIN	{
    print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    print "<elements>"
}

/^[ 	]*[#]/	{}

/^[ 	]*[(]/  {

	group=""
	if (match($0,"[(][0-9a-fA-FxX][0-9a-fA-FxX][0-9a-fA-FxX][0-9a-fA-FxX],")) {
		group=substr($0,RSTART+1,4)
	}
	element=""
	if (match($0,",[0-9a-fA-FxX][0-9a-fA-FxX][0-9a-fA-FxX][0-9a-fA-FxX]")) {
		element=substr($0,RSTART+3,2)
	}
	if (keyword == "VOIBasedPlacementSettingsSequence" && element != "E7") {
		next
	}
	if (keyword == "ReceptorBitsPerPixel" && owner == "Sound Eklin" && group != "0031") {
		next
	}
	block=substr($0,RSTART+1,2)
	if (match($0,"PrivateBlock=\"00")) {
		block=substr($0,RSTART+length("PrivateBlock=\"00"),2)
	}

	ownerattr=""
	if (match($0,"Owner=\"[^\"]*\"")) {
		owner=substr($0,RSTART+length("Owner=\""),
			RLENGTH-length("Owner=\"")-1)
		ownerattr="\" owner=\"" owner
	}

	keywordattr=""
	if (match($0,"Keyword=\"[^\"]*\"")) {
		keyword=substr($0,RSTART+length("Keyword=\""),
			RLENGTH-length("Keyword=\"")-1);
		if (keyword != "?") {
			if (keyword ~ /^3/) {
				keyword="Three" substr(keyword,2)
			}
			gsub(" ","",keyword)
			keywordattr="\" keyword=\"" keyword
		}
	}

	end="\"/>"
	if (match($0,"Name=\"[^\"]*\"")) {
		name=substr($0,RSTART+length("Name=\""),
			RLENGTH-length("Name=\"")-1);
		gsub("\&","\&amp;",name);
		if (name != "?") {
			end="\">" name "</el>"
		}
	}

	match($0,"VR=\"[^\"]*\"");
	vr=substr($0,RSTART+length("VR=\""),RLENGTH-length("VR=\"")-1);
	if (vr == "US\\US or SS\\US") vr = "US or SS";
	if (vr == "OW/OB") vr = "OB or OW";

	match($0,"VM=\"[^\"]*\"");
	vm=substr($0,RSTART+length("VM=\""),RLENGTH-length("VM=\"")-1);

    print "<el tag=\"" group block element ownerattr keywordattr "\" vr=\"" vr "\" vm=\"" vm end
}

END {
    print "</elements>"
}

