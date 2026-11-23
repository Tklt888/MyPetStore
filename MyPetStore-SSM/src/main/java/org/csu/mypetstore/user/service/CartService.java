package org.csu.mypetstore.user.service;

import org.csu.mypetstore.user.domain.*;
import org.csu.mypetstore.user.domain.Views.CartLine;
import org.csu.mypetstore.user.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {
    private final CartItemMapper carts;private final ProductMapper products;public CartService(CartItemMapper c,ProductMapper p){carts=c;products=p;}
    public List<CartLine> lines(long userId){return carts.lines(userId);}public int count(long userId){return carts.count(userId);}public BigDecimal total(List<CartLine> lines){return lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
    @Transactional public void add(long userId,long productId,int quantity){validQty(quantity);Product p=products.lock(productId);if(p==null||!"ON_SALE".equals(p.getStatus()))throw new StoreException("商品不存在或已下架");CartItem old=carts.lockExisting(userId,productId);int next=quantity+(old==null?0:old.getQuantity());validQty(next);if(next>p.getStock())throw new StoreException("库存不足，当前最多可购买 "+p.getStock()+" 件");if(old==null){if(carts.kindCount(userId)>=20)throw new StoreException("购物车最多保存 20 种商品");CartItem item=new CartItem();item.setUserId(userId);item.setProductId(productId);item.setQuantity(quantity);item.setVersion(0L);carts.insert(item);}else if(carts.change(old.getId(),userId,next,old.getVersion())!=1)throw new StoreException("购物车已发生变化，请刷新后重试");}
    @Transactional public void update(long userId,long id,int quantity,long version){validQty(quantity);CartLine line=carts.lines(userId).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(()->new StoreException("购物车条目不存在"));if(quantity>line.stock())throw new StoreException("库存不足，当前最多可购买 "+line.stock()+" 件");if(carts.change(id,userId,quantity,version)!=1)throw new StoreException("购物车已发生变化，请刷新后重试");}
    @Transactional public void remove(long userId,long id,long version){if(carts.removeOwned(id,userId,version)!=1)throw new StoreException("购物车条目已变化，请刷新后重试");}
    private static void validQty(int q){if(q<1||q>99)throw new StoreException("数量应为 1—99 的整数");}
}
