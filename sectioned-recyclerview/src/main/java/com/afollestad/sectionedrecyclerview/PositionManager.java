package com.afollestad.sectionedrecyclerview;

import android.support.annotation.RestrictTo;
import android.support.v4.util.ArrayMap;

import java.util.HashMap;
import java.util.LinkedHashMap;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PositionManager implements SectionedViewHolder.PositionDelegate {

  private final HashMap<Integer, Integer> headerLocationMap;
  private final HashMap<Integer, Integer> footerLocationMap;
  private final HashMap<Integer, Boolean> collapsedSectionMap;
  private ItemProvider itemProvider;
  private boolean hasInvalidated;

  PositionManager() {
    this.headerLocationMap = new LinkedHashMap<>(0);
    this.footerLocationMap = new LinkedHashMap<>(0);
    this.collapsedSectionMap = new LinkedHashMap<>(0);
  }

  boolean hasInvalidated() {
    return hasInvalidated;
  }

  int invalidate(ItemProvider itemProvider) {
    this.hasInvalidated = true;
    this.itemProvider = itemProvider;
    int count = 0;
    headerLocationMap.clear();
    footerLocationMap.clear();
    for (int s = 0; s < itemProvider.getSectionCount(); s++) {
      int itemCount = itemProvider.getItemCount(s);
      if (collapsedSectionMap.get(s) != null) {
        headerLocationMap.put(count, s);
        count += 1;
        continue;
      }
      if (itemProvider.showHeadersForEmptySections() || (itemCount > 0)) {
        headerLocationMap.put(count, s);
        count += itemCount + 1;
        if (itemProvider.showFooters()) {
          footerLocationMap.put(count, s);
          count += 1;
        }
      }
    }
    return count;
  }

  @Override
  public boolean isHeader(int absolutePosition) {
    return headerLocationMap.get(absolutePosition) != null;
  }

  @Override
  public boolean isFooter(int absolutePosition) {
    return footerLocationMap.get(absolutePosition) != null;
  }

  int sectionId(int absolutePosition) {
    Integer result = headerLocationMap.get(absolutePosition);
    if (result == null) {
      return -1;
    }
    return result;
  }

  int footerId(int absolutePosition) {
    Integer result = footerLocationMap.get(absolutePosition);
    if (result == null) {
      return -1;
    }
    return result;
  }

  int sectionHeaderIndex(int section) {
    for (Integer key : headerLocationMap.keySet()) {
      if (headerLocationMap.get(key) == section) {
        return key;
      }
    }
    return -1;
  }

  int sectionFooterIndex(int section) {
    for (Integer key : footerLocationMap.keySet()) {
      if (footerLocationMap.get(key) == section) {
        return key;
      }
    }
    return -1;
  }

  /** Converts an absolute position to a relative position and section. */
  @Override
  public ItemCoord relativePosition(int absolutePosition) {
    Integer absHeaderLoc = headerLocationMap.get(absolutePosition);
    if (absHeaderLoc != null) {
      return new ItemCoord(absHeaderLoc, -1);
    }
    Integer lastSectionIndex = -1;
    for (Integer sectionIndex : headerLocationMap.keySet()) {
      if (absolutePosition > sectionIndex) {
        lastSectionIndex = sectionIndex;
      } else {
        break;
      }
    }
    return new ItemCoord(
        headerLocationMap.get(lastSectionIndex), absolutePosition - lastSectionIndex - 1);
  }

  /**
   * Converts a relative position (index inside of a section) to an absolute position (index out of
   * all items and headers).
   */
  int absolutePosition(int sectionIndex, int relativeIndex) {
    if (sectionIndex < 0 || sectionIndex > itemProvider.getSectionCount() - 1) {
      return -1;
    }
    int sectionHeaderIndex = sectionHeaderIndex(sectionIndex);
    if (relativeIndex > itemProvider.getItemCount(sectionIndex) - 1) {
      return -1;
    }
    return sectionHeaderIndex + (relativeIndex + 1);
  }

  /**
   * Converts a relative position (index inside of a section) to an absolute position (index out of
   * all items and headers).
   */
  int absolutePosition(ItemCoord relativePosition) {
    return absolutePosition(relativePosition.section(), relativePosition.relativePos());
  }

  void expandSection(int section) {
    if (section < 0 || section > itemProvider.getSectionCount() - 1) {
      throw new IllegalArgumentException("Section " + section + " is out of bounds.");
    }
    collapsedSectionMap.remove(section);
  }

  void collapseSection(int section) {
    if (section < 0 || section > itemProvider.getSectionCount() - 1) {
      throw new IllegalArgumentException("Section " + section + " is out of bounds.");
    }
    collapsedSectionMap.put(section, true);
  }

  void toggleSectionExpanded(int section) {
    if (collapsedSectionMap.get(section) != null) {
      expandSection(section);
    } else {
      collapseSection(section);
    }
  }

  void expandAllSections() {
    for (int i = 0; i < itemProvider.getSectionCount(); i++) {
      expandSection(i);
    }
  }

  void collapseAllSections() {
    for (int i = 0; i < itemProvider.getSectionCount(); i++) {
      collapseSection(i);
    }
  }

  boolean isSectionExpanded(int section) {
    if (section < 0 || section > itemProvider.getSectionCount() - 1) {
      throw new IllegalArgumentException("Section " + section + " is out of bounds.");
    }
    return collapsedSectionMap.get(section) == null;
  }
}
