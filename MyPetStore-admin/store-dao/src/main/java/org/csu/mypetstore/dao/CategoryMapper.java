package org.csu.mypetstore.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.csu.mypetstore.model.Category;
import org.csu.mypetstore.model.dto.CategoryRow;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("""
            SELECT id, code, parent_id, name, sort_order
              FROM category
             WHERE parent_id IS NULL
             ORDER BY sort_order ASC, id ASC
            """)
    IPage<CategoryRow> selectTopLevelPage(Page<CategoryRow> page);

    @Select("""
            <script>
            SELECT id, code, parent_id, name, sort_order
              FROM category
             WHERE parent_id IN
             <foreach collection="parentIds" item="parentId" open="(" separator="," close=")">
               #{parentId}
             </foreach>
             ORDER BY sort_order ASC, id ASC
            </script>
            """)
    List<CategoryRow> selectChildrenByParentIds(@Param("parentIds") List<Long> parentIds);

    @Select("SELECT COUNT(*) FROM category WHERE parent_id = #{categoryId}")
    long countChildren(@Param("categoryId") Long categoryId);

    @Select("SELECT COUNT(*) FROM product WHERE category_id = #{categoryId}")
    long countProductReferences(@Param("categoryId") Long categoryId);

    @Select("SELECT id, code, parent_id, name, sort_order FROM category WHERE code = #{code}")
    Category selectByCode(@Param("code") String code);

    @Select("""
            <script>
            SELECT id, code, parent_id, name, sort_order
              FROM category
             WHERE name = #{name}
             <choose>
               <when test="parentId == null">AND parent_id IS NULL</when>
               <otherwise>AND parent_id = #{parentId}</otherwise>
             </choose>
            </script>
            """)
    Category selectByParentAndName(
            @Param("parentId") Long parentId,
            @Param("name") String name);

    @Select("""
            SELECT id, code, parent_id, name, sort_order
              FROM category
             WHERE parent_id IS NULL
             ORDER BY sort_order ASC, id ASC
            """)
    List<CategoryRow> selectTopLevelOptions();

    @Update("""
            UPDATE category
               SET name = #{name}, sort_order = #{sortOrder}
             WHERE id = #{id}
            """)
    int updateNameAndSort(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("sortOrder") Integer sortOrder);
}
