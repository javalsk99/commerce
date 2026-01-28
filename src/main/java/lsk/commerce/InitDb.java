package lsk.commerce;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Member;
import lsk.commerce.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitService initService;

    @PostConstruct
    public void init() {
        initService.dbInit();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final MemberService memberService;

        public void dbInit() {
            Long memberId = memberService.adminJoin(new Member("test", "testId", "testPassword", "seoul", "Gangbuk", "11111"));
        }
    }
}
