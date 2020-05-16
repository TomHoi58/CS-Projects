package edu.berkeley.cs186.database.query;

import java.util.*;

import edu.berkeley.cs186.database.TransactionContext;
import edu.berkeley.cs186.database.common.iterator.BacktrackingIterator;
import edu.berkeley.cs186.database.databox.DataBox;
import edu.berkeley.cs186.database.table.Record;

class SortMergeOperator extends JoinOperator {
    SortMergeOperator(QueryOperator leftSource,
                      QueryOperator rightSource,
                      String leftColumnName,
                      String rightColumnName,
                      TransactionContext transaction) {
        super(leftSource, rightSource, leftColumnName, rightColumnName, transaction, JoinType.SORTMERGE);

        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
    }

    @Override
    public Iterator<Record> iterator() {
        return new SortMergeIterator();
    }

    @Override
    public int estimateIOCost() {
        //does nothing
        return 0;
    }

    /**
     * An implementation of Iterator that provides an iterator interface for this operator.
     *    See lecture slides.
     *
     * Before proceeding, you should read and understand SNLJOperator.java
     *    You can find it in the same directory as this file.
     *
     * Word of advice: try to decompose the problem into distinguishable sub-problems.
     *    This means you'll probably want to add more methods than those given (Once again,
     *    SNLJOperator.java might be a useful reference).
     *
     */
    private class SortMergeIterator extends JoinIterator {
        /**
        * Some member variables are provided for guidance, but there are many possible solutions.
        * You should implement the solution that's best for you, using any member variables you need.
        * You're free to use these member variables, but you're not obligated to.
        */
        private BacktrackingIterator<Record> leftIterator;
        private BacktrackingIterator<Record> rightIterator;
        private Record leftRecord;
        private Record nextRecord;
        private Record rightRecord;
        private boolean marked;
        private boolean detached;


        private SortMergeIterator() {
            super();
            // TODO(proj3_part1): implement

            //Sort both tables and return two iterators

            SortOperator leftoper = new SortOperator(getTransaction(),getLeftTableName(),new LeftRecordComparator());
            this.leftIterator = (BacktrackingIterator<Record>)leftoper.iterator();
            SortOperator rightoper = new SortOperator(getTransaction(),getRightTableName(),new RightRecordComparator());
            this.rightIterator = (BacktrackingIterator<Record>)rightoper.iterator();



            this.nextRecord = null;
            this.leftRecord = null;
            this.rightRecord = null;
            this.marked = false;
            this.detached = false;

            //Fetch left record or error
            if (this.leftIterator.hasNext()){
                this.leftRecord = leftIterator.next();
            }else{
                throw new NoSuchElementException("Empty Table!");
            }
            //Fetch right record or error
            if (this.rightIterator.hasNext()){
                this.rightRecord = rightIterator.next();
            }else{
                throw new NoSuchElementException("Empty Table!");
            }

            try {
                fetchNextRecord();
            } catch (NoSuchElementException e) {
                this.nextRecord = null;
            }
        }

        /**
         * Pre-fetches what will be the next record, and puts it in this.nextRecord.
         * Pre-fetching simplifies the logic of this.hasNext() and this.next()
         */
        private void fetchNextRecord() {
            //nextRecord being fetched is the stopping condition of the while loop
            this.nextRecord = null;
            while(this.nextRecord == null){
                if (!this.marked){
//                    //Fetch left record or break the loop when reach the end of the table
//                    if (this.leftIterator.hasNext()){
//                        this.leftRecord = leftIterator.next();
//                    }else{
//                        System.out.println("The end without mark");
//                        break;
//                    }
//                    //Fetch right record or break the loop when reach the end of the table
//                    if (this.rightIterator.hasNext()){
//                        this.rightRecord = rightIterator.next();
//                    }else{
//                        System.out.println("The end without mark");
//                        break;
//                    }

                    //Advance left iterator and right iterator
                    while(leftRecord.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).compareTo(rightRecord.getValues().get(SortMergeOperator.this.getRightColumnIndex())) < 0){
                        //Advance left iterator and fetch left record or break the loop when reach the end of the table
                        if (this.leftIterator.hasNext()){
                            this.leftRecord = leftIterator.next();
                        }else{
//                            System.out.println("The end without mark");
                            break;
                        }
                    }

                    while(leftRecord.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).compareTo(rightRecord.getValues().get(SortMergeOperator.this.getRightColumnIndex())) > 0){
                        //Advance right iterator and fetch right record or break the loop when reach the end of the table
                        if (this.rightIterator.hasNext()){
                            this.rightRecord = rightIterator.next();
//                            justgotreset = false;
                        }else{

//                            System.out.println("The end without mark");
                            break;
                        }
                    }

                    //Mark the right iterator
                    rightIterator.markPrev();
                    marked = true;

                }

                //Find a match, join them and set it as nextRecord
                if (leftRecord.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).equals(rightRecord.getValues().get(SortMergeOperator.this.getRightColumnIndex())) && !detached){
                    nextRecord = joinRecords(leftRecord,rightRecord);
                    //Advance the right iterator
                    if (rightIterator.hasNext()){
                        rightRecord = rightIterator.next();
                    }
                    else{
                        detached = true;
                        //Reset the right iterator and set marked to false
//                        System.out.println("End of right table");
//                        System.out.println("The end without mark");
//                        rightIterator.reset();
//                        rightRecord = rightIterator.next();
//                        marked = false;
//                        if (leftIterator.hasNext()){
//                            leftRecord = leftIterator.next();
//                        }else{
//                            //Reach the end of left table
//                            System.out.println("The end without mark");
//                            break;
//                        }
                    }

                }else{
                    //Reset the right iterator and set marked to false
                    rightIterator.reset();
                    rightRecord = rightIterator.next();
                    detached = false;
//                    System.out.println("The end without mark");
                    marked = false;
//                    justgotreset = true;
                    if (leftIterator.hasNext()){
                        leftRecord = leftIterator.next();
                    }else{
                        //Reach the end of left table
//                        System.out.println("The end without mark");
                        break;
                    }
                }


            }
        }


        /**
         * Checks if there are more record(s) to yield
         *
         * @return true if this iterator has another record to yield, otherwise false
         */
        @Override
        public boolean hasNext() {
            // TODO(proj3_part1): implement
            return this.nextRecord != null;
        }

        /**
         * Yields the next record of this iterator.
         *
         * @return the next Record
         * @throws NoSuchElementException if there are no more Records to yield
         */
        @Override
        public Record next() {
            // TODO(proj3_part1): implement
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            Record nextRecord = this.nextRecord;
            try {
                this.fetchNextRecord();
            } catch (NoSuchElementException e) {
                this.nextRecord = null;
            }
            return nextRecord;

        }

        /**
         * Helper method to create a joined record from a record of the left relation
         * and a record of the right relation.
         * @param leftRecord Record from the left relation
         * @param rightRecord Record from the right relation
         * @return joined record
         */
        private Record joinRecords(Record leftRecord, Record rightRecord) {
            List<DataBox> leftValues = new ArrayList<>(leftRecord.getValues());
            List<DataBox> rightValues = new ArrayList<>(rightRecord.getValues());
            leftValues.addAll(rightValues);
            return new Record(leftValues);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private class LeftRecordComparator implements Comparator<Record> {
            @Override
            public int compare(Record o1, Record o2) {
                return o1.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).compareTo(
                           o2.getValues().get(SortMergeOperator.this.getLeftColumnIndex()));
            }
        }

        private class RightRecordComparator implements Comparator<Record> {
            @Override
            public int compare(Record o1, Record o2) {
                return o1.getValues().get(SortMergeOperator.this.getRightColumnIndex()).compareTo(
                           o2.getValues().get(SortMergeOperator.this.getRightColumnIndex()));
            }
        }
    }
}
