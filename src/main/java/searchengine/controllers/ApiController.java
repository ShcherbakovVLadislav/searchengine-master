package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.SearchDto;
import searchengine.dto.response.ResultDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.SiteRepository;
import searchengine.search.SearchStarter;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final ApiService apiService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final SitesList sitesList;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;

    private final SearchStarter searchStarter;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResultDto startIndexing() {
        if(indexingProcessing.get()) {
            return new ResultDto(false, "Индексация уже идёт.", HttpStatus.CONFLICT);
        } else {
            indexingProcessing.set(true);
            Runnable start = () -> apiService.startIndexing(indexingProcessing);
            new Thread(start).start();
            return new ResultDto(true, HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResultDto stopIndexing() {
        if (!indexingProcessing.get()) {
            return new ResultDto(false,"Индексация не запущена." , HttpStatus.METHOD_NOT_ALLOWED);
        } else {
            indexingProcessing.set(false);
            return new ResultDto(true, HttpStatus.OK);
        }
    }

    @GetMapping("/indexPage")
    public ResultDto indexPage(@RequestParam URL url) throws IOException {
        try {
            sitesList.getSites().stream().filter(site -> url.getHost().equals(site.getUrl().getHost())).findFirst().orElseThrow();
        } catch (RuntimeException ex) {
            return new ResultDto(false, "Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле.", HttpStatus.BAD_REQUEST);
        }
        lemmaService.getLemmasFromUrl(url);
        return new ResultDto(true, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResultDto search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                            @RequestParam(name = "site", required = false, defaultValue = "") String site,
                            @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {
        List<SearchDto> searchData;
        if (!site.isEmpty()) {
            if (siteRepository.findByUrl(site) == null) {
                return new ResultDto(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
            } else {
                searchData = searchStarter.getSearchFromOneSite(query, site, offset, 20);
            }
        } else {
            searchData = searchStarter.getFullSearch(query, offset, 20);
        }
        return new ResultDto(true, searchData.size(), searchData, HttpStatus.OK);
    }
}
