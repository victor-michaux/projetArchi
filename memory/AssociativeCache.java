package memory;

import java.util.ArrayList;

/**
 * Created by victor on 02/01/2017.
 */
public class AssociativeCache implements Memory{

    private class Entry {
        public int value;
        public int address;
        public boolean isValid;
        public boolean isDirty;
        public int age;

    }

    private final ArrayList<Entry> entries;
    private final int accessTime;
    private final Memory memory;

    private final int size;
    private int operationTime;
    private final Stats stats;

    private int generalAge = 0;

    public AssociativeCache(int size, int accessTime, Memory memory)
    {
        this.size = size;

        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        if (accessTime <= 0) {
            throw new IllegalArgumentException("accessTime");
        }
        if (memory == null) {
            throw new NullPointerException("memory");
        }

        this.accessTime = accessTime;
        this.entries = new ArrayList<Entry>(size);
        this.memory = memory;
        this.stats = new Stats();

    }

    @Override
    public int read(int address) {
        return this.readFIFO(address);
        //return this.readLRU(address);
    }

    @Override
    public void write(int address, int value) {
        this.writeFIFO(address, value);
        //this.writeLRU(address, value);
    }

    @Override
    public int getOperationTime() {
        return this.operationTime;
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    private int readLRU(int address)
    {
        boolean hit = false;
        int value = 0;
        int i = 0;

        // Cherche l'entrée dans le cache
        while (!hit && i < this.entries.size())
        {
            Entry entry = this.entries.get(i);
            if(entry.address == address)
            {
                // Si on trouve l'adresse on lis la valeur
                hit = true;
                value = entry.value;
                updateAge(entry);
                this.operationTime = accessTime;
                this.stats.reads.add(true, this.operationTime);
            }
            i++;
        }

        // Si on trouve pas l'adresse
        if(!hit)
        {
            value = this.memory.read(address);

            Entry entry = new Entry();
            entry.value = value;
            entry.address = address;
            entry.isValid = true;
            entry.isDirty = false;
            updateAge(entry);

            // Si cache est pas complet
            if(this.entries.size() < this.size)
            {
                this.entries.add(entry);
                this.operationTime = accessTime;
                this.stats.reads.add(false, this.operationTime);
            } else {
                // Sinon on enregistre en mémoire centrale la valeur de la dernière adresse utilisée
                this.cacheFullWriteInMemory(entry);
                this.operationTime = this.memory.getOperationTime() + accessTime;
                this.stats.reads.add(false, this.operationTime);
            }
        }

        return value;
    }

    private void writeLRU(int address, int value)
    {
        boolean hit = false;
        int index = 0;
        int i = 0;

        // Cherche l'adresse dans le cache
        while (!hit && i < this.entries.size())
        {
            Entry entry = this.entries.get(i);
            if(entry.address == address)
            {
                hit = true;
                index = i;
            }
            i++;
        }

        if(hit)
        {
            Entry entry = this.entries.get(index);
            if(entry.isDirty)
            {
                this.memory.write(address, value);
            }

            entry.value = value;
            entry.isDirty = true;
            updateAge(entry);

            this.operationTime = this.accessTime;
            this.stats.writes.add(true, this.operationTime);
        } else {
            Entry entry = new Entry();
            entry.isDirty = true;
            entry.value = value;
            entry.address = address;
            entry.isValid = true;
            updateAge(entry);

            if(this.entries.size() < this.size){
                this.entries.add(entry);
                this.operationTime = this.accessTime;
                this.stats.writes.add(false, operationTime);
            } else {
                // Cache plein
                this.cacheFullWriteInMemory(entry);
                this.operationTime = this.memory.getOperationTime() + this.accessTime;
                this.stats.writes.add(false, operationTime);
            }
        }
    }

    private int readFIFO(int address)
    {
        boolean hit = false;
        int value = 0;
        int i = 0;

        // On cherche l'adresse dans le cache
        while (!hit && i < this.entries.size())
        {
            Entry entry = this.entries.get(i);
            if(entry.address == address)
            {
                // Si on trouve l'adresse on lis la valeur
                hit = true;
                value = entry.value;

                this.operationTime = accessTime;
                this.stats.reads.add(true, this.operationTime);
            }
            i++;
        }

        if (!hit){

            Entry entry = new Entry();

            entry.value = memory.read(address);
            entry.address = address;
            entry.isValid = true;
            entry.isDirty = true;
            updateAge(entry);

            value = entry.value;

            // si le cache n'est pas complet
            if (this.entries.size() < this.size){
                this.entries.add(entry);
                this.operationTime = this.accessTime;
                stats.reads.add(false, operationTime);
            } else{
                this.cacheFullWriteInMemory(entry);
                this.operationTime = this.accessTime + this.memory.getOperationTime();
                stats.reads.add(false, operationTime);
            }
        }

        // on retourne la valeur
        return value;
    }

    private void writeFIFO(int address, int value)
    {
        boolean hit = false;
        int index = 0;
        int i = 0;

        // Cherche l'adresse dans le cache
        while (!hit && i < this.entries.size())
        {
            Entry entry = this.entries.get(i);
            if(entry.address == address)
            {
                hit = true;
                index = i;
            }
            i++;
        }

        if(hit){
            Entry entry = entries.get(index);

            // si hit et DIRTY
            if (entry.isDirty){
                this.memory.write(address, value);
                this.operationTime = this.accessTime + this.memory.getOperationTime();
                stats.writes.add(true, this.operationTime);
            }

            entry.value = value;
            entry.isDirty = true;

            //récupération des stats
            operationTime = accessTime;
            stats.writes.add(true, operationTime);

        } else {
            // on créer l'entrée qui va être ajouté au cache
            Entry entry = new Entry();
            entry.isDirty = true;
            entry.value = value;
            entry.address = address;
            updateAge(entry);

            //si le cache n'est pas complet et l'adresse n'est pas en cache, on ajoute directement
            if(entries.size() < this.size){
                this.entries.add(entry);
                this.operationTime = this.accessTime;
                this.stats.writes.add(false, this.operationTime);
            }//si le cache est complet et l'adresse n'est pas en cache, alors il faut chercher l'élément le plus ancien (FIFO), le rajouter à la mémoire puis le remplacer dans le cache
            else{
                this.cacheFullWriteInMemory(entry);
                this.operationTime = this.memory.getOperationTime() + this.accessTime;
                this.stats.writes.add(false, this.operationTime);
            }
        }
    }

    public void flush()
    {
        int entryNumber = 0;
        for(Entry entry : this.entries)
        {
            // Si l'entrée est sale alors on écrit en mémoire centrale
            if(entry.isDirty)
            {
                this.memory.write(entry.address, entry.value);
                this.operationTime = this.memory.getOperationTime() + accessTime;
                this.stats.writes.add(false, this.operationTime);
                this.entries.remove(entry);
            }
            entryNumber++;
        }
    }

    private void cacheFullWriteInMemory(Entry newEntry)
    {
        try {

            int minAgeIndex = getMinAgeIndex(entries);
            Entry oldEntry = entries.get(minAgeIndex);
            memory.write(oldEntry.address, oldEntry.value);
            this.entries.set(minAgeIndex, newEntry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getMinAgeIndex(ArrayList<Entry> entries) throws Exception{
        // infini
        if(entries.size() <= 0)
        {
            throw new Exception("Aucune entree");
        }
        int min = entries.get(0).age;

        int minIndex = 0;
        int index = 0;

        for (Entry entry : entries){
            if (entry.age < min){
                min = entry.age;
                minIndex = index;
            }
            index++;
        }

        return minIndex;
    }

    private void updateAge(Entry entry){
        this.generalAge++;
        entry.age = this.generalAge;
    }
}

