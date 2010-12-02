package org.bson.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;

public class ObjectId implements Serializable {
	private static final long serialVersionUID = 837806216L;
	private static final Random rnd = new Random();
    /** Create a new object id. */
    public ObjectId(){
    	this(new Date());
    }

	public ObjectId( String s ){
        if ( ! isValid( s ) )
            throw new IllegalArgumentException( "invalid ObjectId [" + s + "]" );

        byte b[] = new byte[12];
        for ( int i=0; i<b.length; i++ ){
            b[b.length-(i+1)] = (byte)Integer.parseInt( s.substring( i*2 , i*2 + 2) , 16 );
        }
        ByteBuffer bb = ByteBuffer.wrap( b );
        
        _inc = bb.getInt(); 
        _machine = bb.getInt();
        _time = bb.getInt();

    }
    public ObjectId( Date time ){
        this(time, rnd.nextInt());
    }

    public ObjectId( Date time , int inc ){
    	//TODO: get machine id somehow
        this( time , rnd.nextInt(), inc );
    }

    public ObjectId( Date time , int machine , int inc ){
        _time = _flip( (int)(time.getTime() / 1000) );
        _machine = machine;
        _inc = inc;
    }

	/** Checks if a string could be an <code>ObjectId</code>.
     * @return whether the string could be an object id
     */
    public static boolean isValid( String s ){
        if ( s == null )
            return false;
        
        final int len = s.length();
        if ( len != 24 )
            return false;

        for ( int i=0; i<len; i++ ){
            char c = s.charAt( i );
            if ( c >= '0' && c <= '9' )
                continue;
            if ( c >= 'a' && c <= 'f' )
                continue;
            if ( c >= 'A' && c <= 'F' )
                continue;

            return false;
        }        

        return true;
    }
    public int hashCode(){
        int x = _time;
        x += ( _machine * 111 );
        x += ( _inc * 17 );
        return x;
    }

    public boolean equals( Object o ){
        if ( this == o )
            return true;

        ObjectId other = massageToObjectId( o );
        if ( other == null )
            return false;
        
        return 
            _time == other._time && 
            _machine == other._machine && 
            _inc == other._inc;
    }

    public String toString(){
        byte b[] = toByteArray();

        StringBuilder buf = new StringBuilder(24);
        
        for ( int i=0; i<b.length; i++ ){
            int x = b[i] & 0xFF;
            String s = Integer.toHexString( x );
            if ( s.length() == 1 )
                buf.append( "0" );
            buf.append( s );
        }

        return buf.toString();
    }
    
    /** Turn an object into an <code>ObjectId</code>, if possible.
     * Strings will be converted into <code>ObjectId</code>s, if possible, and <code>ObjectId</code>s will
     * be cast and returned.  Passing in <code>null</code> returns <code>null</code>.
     * @param o the object to convert 
     * @return an <code>ObjectId</code> if it can be massaged, null otherwise 
     */
    public static ObjectId massageToObjectId( Object o ){
        if ( o == null )
            return null;
        
        if ( o instanceof ObjectId )
            return (ObjectId)o;

        if ( o instanceof String ){
            String s = o.toString();
            if ( isValid( s ) )
                return new ObjectId( s );
        }
        
        return null;
    }

    public byte[] toByteArray(){
        byte b[] = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap( b );
        bb.putInt( _inc );
        bb.putInt( _machine );
        bb.putInt( _time );
        reverse( b );
        return b;
    }

    static void reverse( byte[] b ){
        for ( int i=0; i<b.length/2; i++ ){
            byte t = b[i];
            b[i] = b[ b.length-(i+1) ];
            b[b.length-(i+1)] = t;
        }
    }

    public static int _flip( int x ){
        int z = 0;
        z |= ( ( x << 24 ) & 0xFF000000 );
        z |= ( ( x << 8 )  & 0x00FF0000 );
        z |= ( ( x >> 8 )  & 0x0000FF00 );
        z |= ( ( x >> 24 ) & 0x000000FF );
        return z;
    }

    public int getMachine(){
        return _machine;
    }
    
    public long getTime(){
        long z = _flip( _time );
        return z * 1000;
    }

    public int getInc(){
        return _inc;
    }

    final int _time;
    final int _machine;
    final int _inc;
}
