package com.afollestad.sectionedrecyclerview;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class PositionManagerTest {

  private PositionManager positionManager;
  private boolean showFooters;

  @Before
  public void before() {
    showFooters = false;
    positionManager = new PositionManager();
    assertThat(positionManager.hasInvalidated()).isFalse();
    invalidate();
  }

  private int invalidate() {
    return positionManager.invalidate(
        new ItemProvider() {
          @Override
          public int getSectionCount() {
            return 2;
          }

          @Override
          public int getItemCount(int sectionIndex) {
            return 5;
          }

          @Override
          public boolean showHeadersForEmptySections() {
            return true;
          }

          @Override
          public boolean showFooters() {
            return showFooters;
          }
        });
  }

  @Test
  public void test_is_header_true() {
    assertThat(positionManager.isHeader(0)).isTrue();
    assertThat(positionManager.isHeader(6)).isTrue();
  }

  @Test
  public void test_is_header_false() {
    assertThat(positionManager.isHeader(1)).isFalse();
    assertThat(positionManager.isHeader(2)).isFalse();
    assertThat(positionManager.isHeader(3)).isFalse();
    assertThat(positionManager.isHeader(4)).isFalse();
    assertThat(positionManager.isHeader(5)).isFalse();

    assertThat(positionManager.isHeader(7)).isFalse();
    assertThat(positionManager.isHeader(8)).isFalse();
    assertThat(positionManager.isHeader(9)).isFalse();
    assertThat(positionManager.isHeader(10)).isFalse();
    assertThat(positionManager.isHeader(11)).isFalse();
  }

  @Test
  public void test_section_id_no_footers() {
    // Header 0
    // 1
    // 2
    // 3
    // 4
    // 5
    // Header 6
    // 7
    // 8
    // 9
    // 10
    // 11
    assertThat(positionManager.sectionId(-1)).isEqualTo(-1);
    assertThat(positionManager.sectionId(0)).isEqualTo(0);
    assertThat(positionManager.sectionId(1)).isEqualTo(-1);
    assertThat(positionManager.sectionId(5)).isEqualTo(-1);
    assertThat(positionManager.sectionId(6)).isEqualTo(1);
    assertThat(positionManager.sectionId(7)).isEqualTo(-1);
    assertThat(positionManager.sectionId(20)).isEqualTo(-1);
  }

  @Test
  public void test_section_id_with_footers() {
    // Header 0
    // 1
    // 2
    // 3
    // 4
    // 5
    // Footer 6
    // Header 7
    // 8
    // 9
    // 10
    // 11
    // 12
    // Footer 13
    showFooters = true;
    invalidate();
    assertThat(positionManager.sectionId(-1)).isEqualTo(-1);
    assertThat(positionManager.sectionId(0)).isEqualTo(0);
    assertThat(positionManager.sectionId(1)).isEqualTo(-1);
    assertThat(positionManager.sectionId(6)).isEqualTo(-1);
    assertThat(positionManager.sectionId(7)).isEqualTo(1);
    assertThat(positionManager.sectionId(8)).isEqualTo(-1);
    assertThat(positionManager.sectionId(20)).isEqualTo(-1);
  }

  @Test
  public void test_footer_id() {
    showFooters = true;
    invalidate();
    assertThat(positionManager.footerId(5)).isEqualTo(-1);
    assertThat(positionManager.footerId(6)).isEqualTo(0);
    assertThat(positionManager.footerId(7)).isEqualTo(-1);
    assertThat(positionManager.footerId(12)).isEqualTo(-1);
    assertThat(positionManager.footerId(13)).isEqualTo(1);
    assertThat(positionManager.footerId(14)).isEqualTo(-1);
    assertThat(positionManager.footerId(20)).isEqualTo(-1);
  }

  @Test
  public void test_header_index_no_footers() {
    assertThat(positionManager.sectionHeaderIndex(-1)).isEqualTo(-1);
    assertThat(positionManager.sectionHeaderIndex(0)).isEqualTo(0);
    assertThat(positionManager.sectionHeaderIndex(1)).isEqualTo(6);
    assertThat(positionManager.sectionHeaderIndex(2)).isEqualTo(-1);
  }

  @Test
  public void test_header_index_with_footers() {
    showFooters = true;
    invalidate();
    assertThat(positionManager.sectionHeaderIndex(-1)).isEqualTo(-1);
    assertThat(positionManager.sectionHeaderIndex(0)).isEqualTo(0);
    assertThat(positionManager.sectionHeaderIndex(1)).isEqualTo(7);
    assertThat(positionManager.sectionHeaderIndex(2)).isEqualTo(-1);
  }

  @Test
  public void test_relative_pos_no_footers() {
    assertThat(positionManager.relativePosition(0)).isEqualTo(new ItemCoord(0, -1));
    assertThat(positionManager.relativePosition(1)).isEqualTo(new ItemCoord(0, 0));
    assertThat(positionManager.relativePosition(3)).isEqualTo(new ItemCoord(0, 2));

    assertThat(positionManager.relativePosition(6)).isEqualTo(new ItemCoord(1, -1));
    assertThat(positionManager.relativePosition(7)).isEqualTo(new ItemCoord(1, 0));
    assertThat(positionManager.relativePosition(11)).isEqualTo(new ItemCoord(1, 4));
  }

  @Test
  public void test_relative_pos_with_footers() {
    showFooters = true;
    invalidate();

    assertThat(positionManager.relativePosition(0)).isEqualTo(new ItemCoord(0, -1));
    assertThat(positionManager.relativePosition(1)).isEqualTo(new ItemCoord(0, 0));
    assertThat(positionManager.relativePosition(3)).isEqualTo(new ItemCoord(0, 2));

    assertThat(positionManager.relativePosition(7)).isEqualTo(new ItemCoord(1, -1));
    assertThat(positionManager.relativePosition(8)).isEqualTo(new ItemCoord(1, 0));
    assertThat(positionManager.relativePosition(12)).isEqualTo(new ItemCoord(1, 4));
  }

  @Test
  public void test_absolute_pos_no_footers() {
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 0))).isEqualTo(1);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 1))).isEqualTo(2);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 2))).isEqualTo(3);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 3))).isEqualTo(4);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 4))).isEqualTo(5);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 5))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 6))).isEqualTo(-1);

    assertThat(positionManager.absolutePosition(new ItemCoord(1, 0))).isEqualTo(7);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 1))).isEqualTo(8);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 2))).isEqualTo(9);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 3))).isEqualTo(10);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 4))).isEqualTo(11);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 5))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 6))).isEqualTo(-1);

    assertThat(positionManager.absolutePosition(new ItemCoord(2, 6))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(3, 3))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(-5, 3))).isEqualTo(-1);
  }

  @Test
  public void test_absolute_pos_with_footers() {
    showFooters = true;
    invalidate();

    assertThat(positionManager.absolutePosition(new ItemCoord(0, 0))).isEqualTo(1);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 1))).isEqualTo(2);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 2))).isEqualTo(3);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 3))).isEqualTo(4);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 4))).isEqualTo(5);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 5))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(0, 6))).isEqualTo(-1);

    assertThat(positionManager.absolutePosition(new ItemCoord(1, 0))).isEqualTo(8);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 1))).isEqualTo(9);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 2))).isEqualTo(10);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 3))).isEqualTo(11);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 4))).isEqualTo(12);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 5))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(1, 6))).isEqualTo(-1);

    assertThat(positionManager.absolutePosition(new ItemCoord(2, 6))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(3, 3))).isEqualTo(-1);
    assertThat(positionManager.absolutePosition(new ItemCoord(-5, 3))).isEqualTo(-1);
  }

  @Test
  public void test_is_section_expanded_true() {
    assertThat(positionManager.isSectionExpanded(0)).isTrue();
    assertThat(positionManager.isSectionExpanded(1)).isTrue();
  }

  @Test
  public void test_is_section_expanded_false() {
    positionManager.collapseSection(0);
    assertThat(positionManager.isSectionExpanded(0)).isFalse();
    positionManager.collapseSection(1);
    assertThat(positionManager.isSectionExpanded(1)).isFalse();
  }

  @Test
  public void test_collapse_expand_out_of_bounds() {
    try {
      positionManager.collapseSection(-1);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      positionManager.collapseSection(3);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      positionManager.expandSection(-1);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      positionManager.expandSection(3);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      positionManager.isSectionExpanded(-1);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      positionManager.isSectionExpanded(3);
      fail("No exception thrown for out of range section!");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void test_collapse_section() {
    assertThat(invalidate()).isEqualTo(12);
    positionManager.collapseSection(0);
    assertThat(invalidate()).isEqualTo(7);
  }

  @Test
  public void test_collapse_all_sections() {
    assertThat(invalidate()).isEqualTo(12);
    positionManager.collapseAllSections();
    assertThat(invalidate()).isEqualTo(2);
  }

  @Test
  public void test_expand_section() {
    positionManager.collapseSection(1);
    assertThat(invalidate()).isEqualTo(7);
    positionManager.expandSection(1);
    assertThat(invalidate()).isEqualTo(12);
  }

  @Test
  public void test_expand_all_sections() {
    positionManager.collapseAllSections();
    assertThat(invalidate()).isEqualTo(2);
    positionManager.expandAllSections();
    assertThat(invalidate()).isEqualTo(12);
  }

  @Test
  public void test_toggle_expanded() {
    positionManager.collapseSection(1);
    assertThat(invalidate()).isEqualTo(7);
    positionManager.toggleSectionExpanded(1);
    assertThat(invalidate()).isEqualTo(12);
    positionManager.toggleSectionExpanded(1);
    assertThat(invalidate()).isEqualTo(7);
  }

  @Test
  public void test_has_invalidated() {
    assertThat(positionManager.hasInvalidated()).isTrue();
  }

  @Test
  public void test_item_coord_toString() {
    assertThat(new ItemCoord(8, 16).toString()).isEqualTo("8:16");
  }
}
