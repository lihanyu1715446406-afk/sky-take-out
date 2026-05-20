package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    //插入员工数据
    //字段已开启驼峰命名法
    @Insert("insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) " +
            "values "+
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void insert(Employee employee);

    //分页查询，要编写一条sql
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    //根据主键动态修改属性，alt回车去到映射文件里写比较方便
    void update(Employee employee);

    //根据id查询员工信息
    //在select注解里面正常写sql语句就行了
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);
}
