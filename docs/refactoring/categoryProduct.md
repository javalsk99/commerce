# CategoryProduct 리팩토링
## CategoryProductService
- disconnect
  멱등성을 위해 카테고리와 상품이 연결되지 않은 상태일 때, 이후의 로직을 실행하지 않고 반환했다.

- disconnectAll  
  멱등성을 위해 카테고리에 연결된 상품이 존재하지 않을 때, 이후의 로직을 실행하지 않고 반환했다.