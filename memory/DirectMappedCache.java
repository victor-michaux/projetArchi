package memory;

import java.util.ArrayList;

import util.Utils;

//
// CACHE ENTRY
//
class Entry {
  int value;        // the line's word (1 cache-line = 1 word)
  int tag;          // the line's tag
  boolean isValid;  // the validity bit
    boolean isDirty; //Dirty bit
}

public class DirectMappedCache implements Memory {

  //
  // ATTRIBUTES
  //
  private final ArrayList<Entry> entries;
  private final int accessTime;
  private final Memory memory;

  private final int indexWidth;
  private final int indexMask;

  private int operationTime;
  private final Stats stats;

  //
  // CONSTRUCTORS
  //
  public DirectMappedCache(int size, int accessTime, Memory memory) {
    if (size <= 0) {
      throw new IllegalArgumentException("size");
    }
    if (accessTime <= 0) {
      throw new IllegalArgumentException("accessTime");
    }
    if (memory == null) {
      throw new NullPointerException("memory");
    }
    indexWidth = Utils.log(size);
    if (indexWidth == -1) {
      throw new IllegalArgumentException("size");
    }
    this.indexMask = Utils.mask(indexWidth);

    this.entries = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      entries.add(new Entry());
    }
    this.accessTime = accessTime;
    this.memory = memory;

    this.stats = new Stats();
  }

  //
  // Memory INTERFACE
  //
  @Override
  public int read(int address) {
    Entry entry = entries.get(toIndex(address));
    if (entry.isValid && entry.tag == toTag(address)) {
      // hit
      operationTime = accessTime;
      stats.reads.add(true, operationTime);
    } else {
      // miss
      entry.value = memory.read(address);
      entry.tag = toTag(address);
      entry.isValid = true;
      operationTime = memory.getOperationTime() + accessTime;
      stats.reads.add(false, operationTime);
    }
    return entry.value;
  }

  @Override
  public void write(int address, int value) {
    writeAround(address, value);
  }

  private void writeAround(int address, int value) {
      // Ecriture en mémoire centrale
      memory.write(address, value);
      operationTime = memory.getOperationTime() + accessTime;
      stats.writes.add(false, operationTime);

      Entry entry = entries.get(toIndex(address));

      // Si addresse correspond a une entree en cache alors cette entrée n'est plus valide
      if (entry.isValid && entry.tag == toTag(address)) {
          entry.isValid = false;
          this.operationTime = this.accessTime;
          this.stats.writes.add(true, this.operationTime);
      }
  }

  private void writeThrough(int address, int value) {
      // Ecriture en mémoire centrale
      this.memory.write(address, value);
      this.operationTime = this.accessTime + memory.getOperationTime();
      this.stats.writes.add(true, this.operationTime);

      Entry entry = this.entries.get(toIndex(address));

      // Ecriture dans le cache
      entry.tag = toTag(address);
      entry.value = value;
      entry.isValid = true;

      this.entries.set(toIndex(address), entry);
  }

  private void writeBack(int address, int value) {
      Entry entry = this.entries.get(toIndex(address));

      // Si addresse présent dans cache, on met a jour juste la valeur
      if(entry.isValid && entry.tag == toTag(address))
      {
          entry.value = value;
          entry.isDirty = true;
          this.operationTime = accessTime;
          stats.writes.add(true, this.operationTime);
      } else {
          // Si la valeur de l'entrée est pas écrite (dirty) en mémoire centrale alors on le fait
          if(entry.isDirty)
          {
              int adresse = toAddress(entry.tag, toIndex(address));
              this.memory.write(adresse, entry.value);
          }

          // Mise a jour de l'entrée
          entry.value = value;
          entry.tag = toTag(address);
          entry.isValid = true;
          entry.isDirty = true;

          this.operationTime = this.accessTime + this.memory.getOperationTime();
          this.stats.writes.add(false, this.operationTime);

      }

      this.entries.set(toIndex(address), entry);
  }

  // Enregistre le cache en mémoire centrale
  public void flush()
  {
      int entryNumber = 0;
      for(Entry entry : this.entries)
      {
          // Si l'entrée est sale alors on écrit en mémoire centrale
          if(entry.isDirty)
          {
              this.memory.write(toAddress(entry.tag, entryNumber), entry.value);
              this.operationTime = this.memory.getOperationTime() + accessTime;
              this.stats.writes.add(false, this.operationTime);
              this.entries.remove(entry);
          }
          entryNumber++;
      }
  }

  @Override
  public int getOperationTime() {
    return operationTime;
  }

  @Override
  public Stats getStats() {
    return stats;
  }

  //
  // UTILITIES
  //
  private int toIndex(int address) {
    return address & indexMask;
  }

  private int toTag(int address) {
    return address >> indexWidth;
  }

  private int toAddress(int tag, int index) {
    return (tag << indexWidth) + index;
  }

}
