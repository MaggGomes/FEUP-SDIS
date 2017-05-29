package com.chat.herechat.Peer;


public class Peer {
	public String uniqueID;
	public String IPaddress;
	public String name;

	public Peer(String name, String uniqueID, String IPaddress) {
		this.uniqueID = uniqueID;
		this.IPaddress = IPaddress;
		this.name = name;
	}
	
	@Override
	public boolean equals(Object other_) {
		if(other_==null || !( other_ instanceof Peer) ){return false;}
		Peer other = (Peer)other_;
		
		if(uniqueID==null && IPaddress ==null && name==null && other.uniqueID==null && other.IPaddress ==null && other.name==null){return true;}
		
		if((other.uniqueID==null && uniqueID!=null)||(uniqueID==null && other.uniqueID!=null)){return false;}
		
		if((other.IPaddress ==null && IPaddress !=null)||(IPaddress ==null && other.IPaddress !=null)){return false;}

		if((other.name==null && name!=null)||(name==null && other.name!=null)){return false;}

		
		if (other.uniqueID!=null && !other.uniqueID.equalsIgnoreCase(this.uniqueID))
			return false;
		if (other.IPaddress !=null && !other.IPaddress.equalsIgnoreCase(this.IPaddress))
			return false;
        return !(other.name != null && !other.name.equalsIgnoreCase(this.name));

    }
}
