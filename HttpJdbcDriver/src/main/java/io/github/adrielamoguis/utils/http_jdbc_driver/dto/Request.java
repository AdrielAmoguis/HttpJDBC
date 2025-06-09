package main.java.io.github.adrielamoguis.utils.http_jdbc_driver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Request {

    private String query;
    private List<Object> params;

}
