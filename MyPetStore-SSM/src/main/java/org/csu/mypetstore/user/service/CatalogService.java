package org.csu.mypetstore.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.user.domain.Views.*;
import org.csu.mypetstore.user.mapper.CatalogMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CatalogService {
    private static final Set<String>TYPES=Set.of("ALL","PETS","SUPPLIES"),SORTS=Set.of("DEFAULT","PRICE_ASC","PRICE_DESC","NEWEST");
    private final CatalogMapper mapper; public CatalogService(CatalogMapper mapper){this.mapper=mapper;}
    public List<CategoryView> categories(){return mapper.categories();}
    public List<ProductCard> newest(){return mapper.newest(4).stream().map(this::polish).toList();} public List<ProductCard> featured(){return mapper.featured(4).stream().map(this::polish).toList();}
    public IPage<ProductCard> search(String keyword,Long categoryId,String type,String sort,long page,long size){
        keyword=keyword==null?null:keyword.strip();if(keyword!=null&&keyword.length()>120)throw new StoreException("搜索关键字不能超过 120 个字符");
        type=type==null||type.isBlank()?"ALL":type;sort=sort==null||sort.isBlank()?"DEFAULT":sort;
        if(!TYPES.contains(type))throw new StoreException("商品类型参数不正确");if(!SORTS.contains(sort))throw new StoreException("排序参数不正确");if(page<1)page=1;if(size<1||size>50)throw new StoreException("每页数量应在 1—50 之间");
        if(categoryId!=null&&mapper.category(categoryId)==null)throw new StoreException("分类不存在或不可用");
        IPage<ProductCard> result=mapper.search(new Page<>(page,size),escapeLike(keyword),categoryId,type,sort);
        result.setRecords(result.getRecords().stream().map(this::polish).toList());return result;
    }
    public ProductDetail detail(long id){ProductDetailRow d=mapper.detail(id);if(d==null)throw new StoreException("商品不存在或已下架");String cover=displayCover(d.coverImage());List<ImageView> images=cover.startsWith("images/catalog/")?List.of(new ImageView(0L,cover,d.name())):mapper.images(id);return new ProductDetail(d.id(),d.sku(),d.name(),polishSummary(d.name(),d.summary(),d.categoryName()),polishDescription(d.name(),d.description(),d.categoryName()),cover,d.price(),d.stock(),d.categoryId(),d.categoryName(),d.parentId(),d.parentName(),images.isEmpty()?List.of(new ImageView(0L,cover,d.name())):images);}
    private ProductCard polish(ProductCard p){return new ProductCard(p.id(),p.sku(),p.name(),polishSummary(p.name(),p.summary(),p.categoryName()),displayCover(p.coverImage()),p.price(),p.stock(),p.categoryId(),p.categoryName(),p.parentId(),p.parentName(),p.createdAt());}
    private static String displayCover(String path){return path!=null&&path.startsWith("images/products/")&&path.endsWith("/1.jpg")?path.substring(0,path.length()-5)+"cover-light.png":path;}
    private String polishSummary(String name,String text,String category){if(!isTeachingCopy(text))return text;boolean pet=category!=null&&category.contains("宠物");return pet?name+"品种展示，性格友好，适合作为家庭陪伴伙伴。":name+"，兼顾实用性与舒适体验，适合日常宠物生活。";}
    private String polishDescription(String name,String text,String category){if(!isTeachingCopy(text))return text;boolean pet=category!=null&&category.contains("宠物");return pet?name+"拥有鲜明的品种特征与亲和外形。页面信息用于帮助了解宠物特点，选择前请结合实际饲养空间、时间与照护条件。":name+"采用贴近日常使用场景的设计，注重安全、耐用与易清洁。请根据宠物体型和生活习惯选择合适的用品。";}
    private static boolean isTeachingCopy(String text){return text==null||text.isBlank()||text.contains("教学")||text.contains("演示")||text.contains("原型")||text.contains("同分类")||text.contains("初始库存")||text.contains("并发")||text.contains("事务");}
    private static String escapeLike(String value){return value==null?null:value.replace("=","==").replace("%","=%").replace("_","=_");}
}
