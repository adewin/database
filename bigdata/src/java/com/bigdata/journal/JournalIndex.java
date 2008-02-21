/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.bigdata.journal;

import java.util.UUID;

import com.bigdata.btree.BTree;
import com.bigdata.btree.Checkpoint;
import com.bigdata.btree.IKeyBuilder;
import com.bigdata.btree.IndexMetadata;
import com.bigdata.btree.KeyBuilder;
import com.bigdata.io.SerializerUtil;
import com.bigdata.mdi.IResourceMetadata;
import com.bigdata.rawstore.Bytes;
import com.bigdata.rawstore.IRawStore;

/**
 * BTree mapping {@link IJournal} commit times to {@link IResourceMetadata}
 * records. The keys are the long integers. The values are
 * {@link IResourceMetadata} objects.
 * <p>
 * Note: Access to this object MUST be synchronized.
 * <p>
 * Note: This is used as a transient data structure that is populated from the
 * file system by the {@link ResourceManager}.
 */
public class JournalIndex extends BTree {

    /**
     * Instance used to encode the timestamp into the key.
     */
    private IKeyBuilder keyBuilder = new KeyBuilder(Bytes.SIZEOF_LONG);

    /**
     * Create a new instance.
     * 
     * @param store
     *            The backing store.
     * 
     * @return The new instance.
     */
    static public JournalIndex create(IRawStore store) {
    
        IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
        
        metadata.setClassName(JournalIndex.class.getName());
        
        return (JournalIndex) BTree.create(store, metadata);
        
    }

    /**
     * Load from the store.
     * 
     * @param store
     *            The backing store.
     * @param checkpoint
     *            The {@link Checkpoint} record.
     * @param metadataId
     *            The metadata record for the index.
     */
    public JournalIndex(IRawStore store, Checkpoint checkpoint, IndexMetadata metadata) {

        super(store, checkpoint, metadata);

    }
    
    /**
     * Encodes the commit time into a key.
     * 
     * @param commitTime
     *            The commit time.
     * 
     * @return The corresponding key.
     */
    protected byte[] getKey(long commitTime) {

        /*
         * Note: The {@link UnicodeKeyBuilder} is NOT thread-safe
         */
        return keyBuilder.reset().append(commitTime).getKey();

    }

//    /**
//     * Existence test for an index entry with the specified commit timestamp
//     * (exact match).
//     * 
//     * @param commitTime
//     *            The commit timestamp.
//     * 
//     * @return true iff such an {@link IResourceMetadata} entry exists in the
//     *         index with that commit timestamp (exact match).
//     */
//    synchronized public boolean hasTimestamp(long commitTime) {
//        
//        return super.contains(getKey(commitTime));
//        
//    }
//    
//    /**
//     * Return the {@link IResourceMetadata} with the given timestamp (exact
//     * match).
//     * 
//     * @param commitTime
//     *            The commit time.
//     * 
//     * @return The {@link IResourceMetadata} record for that timestamp or
//     *         <code>null</code> iff there is no entry for for that timestamp.
//     */
//    synchronized public IResourceMetadata get(long commitTime) {
//
//        // exact match index lookup.
//        final byte[] val = super.lookup(getKey(commitTime));
//
//        if (val == null) {
//
//            // nothing under that key.
//            return null;
//            
//        }
//        
//        // deserialize the entry.
//        final IResourceMetadata entry = deserializeEntry(new DataInputBuffer(val));
//
//        // return entry.
//        return entry;
//
//    }

    /**
     * Return the {@link IResourceMetadata} identifying the journal having the
     * largest createTime that is less than or equal to the given timestamp.
     * This is used primarily to locate the commit record that will serve as the
     * ground state for a transaction having <i>timestamp</i> as its start
     * time. In this context the LTE search identifies the most recent commit
     * state that not later than the start time of the transaction.
     * 
     * @param timestamp
     *            The given timestamp.
     * 
     * @return The description of the relevant journal resource -or-
     *         <code>null</code> iff there are no journals in the index that
     *         satisify the probe.
     * 
     * @throws IllegalArgumentException
     *             if <i>timestamp</i> is less than or equal to ZERO (0L).
     */
    synchronized public IResourceMetadata find(long timestamp) {

        if (timestamp <= 0L)
            throw new IllegalArgumentException();
        
        // find (first less than or equal to).
        final int index = findIndexOf(timestamp);
        
        if(index == -1) {
            
            // No match.
            
            return null;
            
        }

        /*
         * Retrieve the entry from the index.
         */
        final IResourceMetadata entry = deserializeEntry( super.valueAt( index ) );

        return entry;

    }
    
    /**
     * Find the index of the {@link ICommitRecord} having the largest timestamp
     * that is less than or equal to the given timestamp.
     * 
     * @return The index of the {@link ICommitRecord} having the largest
     *         timestamp that is less than or equal to the given timestamp -or-
     *         <code>-1</code> iff there are no {@link ICommitRecord}s
     *         defined.
     */
    synchronized public int findIndexOf(long timestamp) {
        
        int pos = super.indexOf(getKey(timestamp));
        
        if (pos < 0) {

            /*
             * the key lies between the entries in the index, or possible before
             * the first entry in the index. [pos] represents the insert
             * position. we convert it to an entry index and subtract one to get
             * the index of the first commit record less than the given
             * timestamp.
             */
            
            pos = -(pos+1);

            if(pos == 0) {

                // No entry is less than or equal to this timestamp.
                return -1;
                
            }
                
            pos--;

            return pos;
            
        } else {
            
            /*
             * exact hit on an entry.
             */
            
            return pos;
            
        }

    }
    
    /**
     * Add an entry under the commitTime associated with the
     * {@link IResourceMetadata} record.
     * 
     * @param resourceMetadata
     *            The {@link IResourceMetadata} record.
     * 
     * @exception IllegalArgumentException
     *                if <i>commitTime</i> is <code>0L</code>.
     * @exception IllegalArgumentException
     *                if <i>resourceMetadata</i> is <code>null</code>.
     * @exception IllegalArgumentException
     *                if there is already an entry registered under for the
     *                given timestamp.
     */
    synchronized public void add(IResourceMetadata resourceMetadata) {

        if (resourceMetadata == null)
            throw new IllegalArgumentException();

        final long createTime = resourceMetadata.getCreateTime();

        if (createTime == 0L)
            throw new IllegalArgumentException();

        final byte[] key = getKey(createTime);
        
        if(super.contains(key)) {
            
            throw new IllegalArgumentException("entry exists: timestamp="
                    + createTime);
            
        }
        
        // add a serialized entry to the persistent index.
        super.insert(key, serializeEntry(resourceMetadata));
        
    }
    
    /**
     * Serialize an index entry.
     * 
     * @param entry
     *            The entry.
     * 
     * @return The serialized entry.
     */
    protected byte[] serializeEntry(IResourceMetadata entry) {

        if (entry == null)
            throw new IllegalArgumentException();

        return SerializerUtil.serialize(entry);

    }

    /**
     * De-serialize an index entry
     * 
     * @param is
     *            The serialized data.
     * 
     * @return The entry.
     */
    protected IResourceMetadata deserializeEntry(byte[] data ) {

        return (IResourceMetadata) SerializerUtil.deserialize(data);
        
    }
    
}
