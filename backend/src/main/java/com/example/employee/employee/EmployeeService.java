package com.example.employee.employee;

import com.example.employee.error.ConflictException;
import com.example.employee.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public List<Employee> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Employee findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません：" + id));
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("このメールアドレスはすでに登録されています：" + request.email());
        }
        return repository.save(new Employee(
                request.name(),
                request.email(),
                request.department(),
                request.salary(),
                request.hireDate()
        ));
    }

    @Transactional
    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = findById(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("このメールアドレスはすでに登録されています：" + request.email());
        }

        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setSalary(request.salary());
        employee.setHireDate(request.hireDate());
        return employee;
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
